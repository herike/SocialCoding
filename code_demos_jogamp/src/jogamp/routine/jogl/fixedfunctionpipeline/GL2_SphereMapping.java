package jogamp.routine.jogl.fixedfunctionpipeline;

/**
 **   __ __|_  ___________________________________________________________________________  ___|__ __
 **  //    /\                                           _                                  /\    \\  
 ** //____/  \__     __ _____ _____ _____ _____ _____  | |     __ _____ _____ __        __/  \____\\ 
 **  \    \  / /  __|  |     |   __|  _  |     |  _  | | |  __|  |     |   __|  |      /\ \  /    /  
 **   \____\/_/  |  |  |  |  |  |  |     | | | |   __| | | |  |  |  |  |  |  |  |__   "  \_\/____/   
 **  /\    \     |_____|_____|_____|__|__|_|_|_|__|    | | |_____|_____|_____|_____|  _  /    /\     
 ** /  \____\                       http://jogamp.org  |_|                              /____/  \    
 ** \  /   "' _________________________________________________________________________ `"   \  /    
 **  \/____.                                                                             .____\/     
 **
 ** Simple GL2-Profile demonstration using wavefront object loading, display-lists, automatic
 ** texture coordinate generation (sphere-mapping) and basic materials and lighting. Also uses 
 ** OffsetTableUtils to precalculate some nice cosinus based object movement. Enables the user
 ** to play around the post-processing filter infrastructure using keys 1+2,3+4,5+6 and 7+8 to
 ** switch between different prefilters, linear convolutions and blending modes. For an impression
 ** how this routine looks like see here: http://www.youtube.com/watch?v=ceEF5Z5K3lE
 **
 **/

import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.jogamp.opengl.util.gl2.*;
import com.jogamp.opengl.util.texture.*;
import framework.base.*;
import framework.util.*;
import static javax.media.opengl.GL2.*;

public class GL2_SphereMapping extends BaseRoutineAdapter implements BaseRoutineInterface {

    private BasePostProcessingFilterChainExecutor mBasePostProcessingFilterChainExecutor;
    private ArrayList<BasePostProcessingFilterChainShaderInterface> mPreFilters;
    private ArrayList<BasePostProcessingFilterChainShaderInterface> mBlenders;
    private ArrayList<BasePostProcessingFilterChainShaderInterface> mConvolutions;
    private int mDisplayListID;
    private float[] mOffsetSinTable;
    private static final int OFFSETSINTABLE_SIZE = 2700;
    private Texture mTexture;

    public void initRoutine(GL2 inGL,GLU inGLU,GLUT inGLUT) {
        mOffsetSinTable = OffsetTableUtils.cosaque_SinglePrecision(OFFSETSINTABLE_SIZE,360,true,OffsetTableUtils.TRIGONOMETRIC_FUNCTION.SIN);
        mTexture = TextureUtils.loadImageAsTexture_UNMODIFIED("/binaries/textures/Spheremap_Uffizi_Gallery_MedRes.png");
        mDisplayListID = WavefrontObjectLoader.loadWavefrontObjectAsDisplayList(inGL,"/binaries/geometry/DiscoSphere.wobj.zip");
        //setup post-processing filter chain ...
        mBasePostProcessingFilterChainExecutor = new BasePostProcessingFilterChainExecutor();
        mBasePostProcessingFilterChainExecutor.init(inGL,inGLU,inGLUT);
        mPreFilters = PostProcessingUtils.generatePreFilterArrayList(inGL,inGLU,inGLUT);
        mConvolutions = PostProcessingUtils.generatePostProcessingFilterArrayList(inGL,inGLU,inGLUT);
        mBlenders = PostProcessingUtils.generateBlenderFilterArrayList(inGL,inGLU,inGLUT);
        inGL.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, DirectBufferUtils.createDirectFloatBuffer(new float[]{0.0f,0.0f,0.0f,0.0f}));
        inGL.glLightfv(GL_LIGHT0, GL_AMBIENT, DirectBufferUtils.createDirectFloatBuffer(new float[]{0.25f,0.25f,0.25f,1.0f}));
        inGL.glLightfv(GL_LIGHT0, GL_DIFFUSE, DirectBufferUtils.createDirectFloatBuffer(new float[]{1.0f,1.0f,1.0f,1.0f}));
        inGL.glLightfv(GL_LIGHT0, GL_SPECULAR, DirectBufferUtils.createDirectFloatBuffer(new float[]{1.0f,1.0f,1.0f,1.0f}));
        inGL.glLightfv(GL_LIGHT0, GL_POSITION, DirectBufferUtils.createDirectFloatBuffer(new float[]{-100.0f,100.0f,50.0f,1.0f}));
        inGL.glEnable(GL_LIGHTING);
        inGL.glEnable(GL_LIGHT0);
        inGL.glMaterialfv(GL_FRONT, GL_SPECULAR, DirectBufferUtils.createDirectFloatBuffer(new float[]{1.0f,1.0f,1.0f,1.0f}));
        inGL.glMateriali(GL_FRONT, GL_SHININESS, 12);
    }

    public void mainLoop(int inFrameNumber,GL2 inGL,GLU inGLU,GLUT inGLUT) {
        inGL.glMatrixMode(GL_MODELVIEW);
        inGL.glLoadIdentity();    
        inGL.glTranslatef(0.0f,-0.45f,0.0f);
        inGL.glCullFace(GL_BACK);
        inGL.glFrontFace(GL_CCW);
        inGL.glEnable(GL_CULL_FACE);
        inGL.glEnable(GL_DEPTH_TEST);
        //decal texture environment  ...
        inGL.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_DECAL);	
        inGL.glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL,GL_SEPARATE_SPECULAR_COLOR);
        inGL.glEnable(GL_COLOR_SUM);
        mTexture.enable();
        mTexture.bind();
        mTexture.setTexParameterf(GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        mTexture.setTexParameterf(GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        mTexture.setTexParameterf(GL_TEXTURE_WRAP_S,GL_REPEAT);
        mTexture.setTexParameterf(GL_TEXTURE_WRAP_T,GL_REPEAT);
        TextureUtils.preferAnisotropicFilteringOnTextureTarget(inGL,GL_TEXTURE_2D);
        //turn on texture coordiante generation ...
        inGL.glEnable(GL_TEXTURE_GEN_S);
        inGL.glEnable(GL_TEXTURE_GEN_T);
        //spheremap will be the default ...
        inGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_SPHERE_MAP);
        inGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_SPHERE_MAP);
        inGL.glPushMatrix();
            inGL.glTranslatef(0.0f, 0.45f, -2.75f);
            inGL.glRotatef(mOffsetSinTable[(inFrameNumber)%OFFSETSINTABLE_SIZE], 0.25f, 1.0f, 0.5f);
            inGL.glRotatef(mOffsetSinTable[(int)(inFrameNumber*1.5f)%OFFSETSINTABLE_SIZE], 0.75f, 0.3f, 0.1f);
            inGL.glCallList(mDisplayListID);
        inGL.glPopMatrix();
        mTexture.disable();
        inGL.glDisable(GL_COLOR_SUM);
        inGL.glDisable(GL_TEXTURE_GEN_S);
        inGL.glDisable(GL_TEXTURE_GEN_T);
        //build postprocessing filterchain and execute ...
        mBasePostProcessingFilterChainExecutor.removeAllFilters();
        int tCurrentPreFilterIndex = Math.abs(BaseGlobalEnvironment.getInstance().getParameterKey_INT_78()%mPreFilters.size());
        mBasePostProcessingFilterChainExecutor.addFilter(mPreFilters.get(tCurrentPreFilterIndex));
        int tCurrentConvolutionIndex = Math.abs(BaseGlobalEnvironment.getInstance().getParameterKey_INT_34()%mConvolutions.size());
        BasePostProcessingFilterChainShaderInterface tCurrentShader = mConvolutions.get(tCurrentConvolutionIndex);
        tCurrentShader.setNumberOfIterations(Math.abs(25+BaseGlobalEnvironment.getInstance().getParameterKey_INT_56()));
        mBasePostProcessingFilterChainExecutor.addFilter(mConvolutions.get(tCurrentConvolutionIndex));
        int tCurrentBlenderIndex = Math.abs(BaseGlobalEnvironment.getInstance().getParameterKey_INT_12()%mBlenders.size());
        mBasePostProcessingFilterChainExecutor.addFilter(mBlenders.get(tCurrentBlenderIndex));	
        mBasePostProcessingFilterChainExecutor.executeFilterChain(inGL,inGLU,inGLUT);
    }

    public void cleanupRoutineJOGL(GL2 inGL,GLU inGLU,GLUT inGLUT) {
        inGL.glDeleteLists(mDisplayListID,1);
        inGL.glFlush();
        mTexture.destroy(inGL);
    }

}
