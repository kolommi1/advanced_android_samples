package cz.uhk.advanced_android_samples.object_visualization.oglutils;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES31;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public final class ShaderUtils {
	private static final String TAG = "OGLShaderutils";

	public static final String VERTEX_SHADER_EXTENSION = ".vert";
	public static final String FRAGMENT_SHADER_EXTENSION = ".frag";
	public static final String COMPUTE_SHADER_EXTENSION = ".comp";

	public static final int VERTEX_SHADER_SUPPORT_VERSION = 100;
	public static final int FRAGMENT_SHADER_SUPPORT_VERSION = 100;
	public static final int COMPUTE_SHADER_SUPPORT_VERSION = 310;

	private static final String[] SHADER_FILE_EXTENSIONS = { VERTEX_SHADER_EXTENSION, FRAGMENT_SHADER_EXTENSION,
			COMPUTE_SHADER_EXTENSION };

	private static final int[] SHADER_SUPPORT_EXTENSIONS = { VERTEX_SHADER_SUPPORT_VERSION,
			FRAGMENT_SHADER_SUPPORT_VERSION, COMPUTE_SHADER_SUPPORT_VERSION };

	private static final int[] SHADER_NAME_CONSTANTS = { GLES20.GL_VERTEX_SHADER, GLES20.GL_FRAGMENT_SHADER, GLES31.GL_COMPUTE_SHADER };

	private static final String[] SHADER_NAMES = { "Vertex", "Fragment", "Compute" };


	/**
	 * Load, create, compile, attach and link shader sources defined as files
	 *
	 * @param vertexShaderFileName
	 *            full path name of vertex shader file with/without file
	 *            extension (VERTEX_SHADER_EXTENSION) or null
	 * @param fragmentShaderFileName
	 *            full path name of fragment shader file with/without file
	 *            extension (FRAGMENT_SHADER_EXTENSION) or null
	 * @param computeShaderFileName
	 *            full path name of compute shader file with/without file
	 *            extension (COMPUTE_SHADER_EXTENSION) or null
	 * @return new id of shader program
	 */
	public static int loadProgram(Context context, int maxGlEsVersion, String vertexShaderFileName, String fragmentShaderFileName,
                                  String computeShaderFileName) {
		String[] shaderFileNames = new String[SHADER_FILE_EXTENSIONS.length];
		shaderFileNames[0] = vertexShaderFileName;
		shaderFileNames[1] = fragmentShaderFileName;
		shaderFileNames[2] = computeShaderFileName;
		return loadProgram(context, maxGlEsVersion, shaderFileNames);
	}

	/**
	 * Load, create, compile, attach and link shader sources defined as files
	 *
	 * @param shaderFileName
	 *            full path name of shader file without file extension
	 * @return new id of shader program
	 */
	public static int loadProgram(Context context, int maxGlEsVersion, String shaderFileName) {
		String[] shaderFileNames = new String[SHADER_FILE_EXTENSIONS.length];
		for (int i = 0; i < SHADER_FILE_EXTENSIONS.length; i++)
			shaderFileNames[i] = shaderFileName;
		return loadProgram(context, maxGlEsVersion, shaderFileNames);
	}

	/**
	 * Load, create, compile, attach and link shader sources defined as files
	 *
	 * @param shaderFileNames
	 *            array of full path name of shader files with/without file
	 *            extension in order vertex, fragment, compute shader or null
	 * 
	 * @return new id of shader program
	 */
	public static int loadProgram(Context context, int maxGlEsVersion, String[] shaderFileNames) {
		if (shaderFileNames.length > SHADER_NAMES.length) {
			Log.e(TAG,"Number of shader sources is bigger than number of shaders");
			return -1;
		}
		String[] shaderSrcArray = new String[SHADER_FILE_EXTENSIONS.length];
		for (int i = 0; i < shaderFileNames.length; i++) {
			if (shaderFileNames[i] == null)
				continue;

			String shaderFileName = shaderFileNames[i];
			int index = shaderFileNames[i].indexOf(".");
			if (index < 0) // file extension added
				shaderFileName += SHADER_FILE_EXTENSIONS[i];

			Log.i(TAG,"Shader file: " + shaderFileName + " Reading ... ");

			String shaderSrc = readShaderProgram(context, shaderFileName);
			if (shaderSrc == null) {
				continue;
			} else {
				Log.i(TAG,"OK");
			}
			shaderSrcArray[i] = shaderSrc;
		}

		return loadProgramFromSource(maxGlEsVersion, shaderSrcArray);
	}

	/**
	 * Load, create, compile, attach and link shader sources defined Strings
	 *
	 * @param vertexShaderSrc
	 *           String with GLSL code for vertex shader or null
	 * @param fragmentShaderSrc
	 *            String with GLSL code for fragment shader or null
	 * @param computeShaderSrc
	 *            String with GLSL code for compute shader or null
	 * @return new id of shader program
	 */
	public static int loadProgramFromSource(int maxGlEsVersion, String vertexShaderSrc, String fragmentShaderSrc,
                                            String computeShaderSrc) {
		String[] shaderSrcArray = new String[SHADER_FILE_EXTENSIONS.length];
		shaderSrcArray[0] = vertexShaderSrc;
		shaderSrcArray[1] = fragmentShaderSrc;
		shaderSrcArray[2] = computeShaderSrc;
		return loadProgramFromSource(maxGlEsVersion, shaderSrcArray);
	}

	/**
	 * Load, create, compile, attach and link shader sources defined as Strings
	 * @param shaderSrcArray
	 *            arrays of Strings with GLSL codes for shaders in
	 *            order vertex, fragment, compute shader or null
	 * @return new id of shader program
	 */
	public static int loadProgramFromSource(int maxGlEsVersion, String[] shaderSrcArray) {
		OGLUtils.emptyGLError();
		if (shaderSrcArray.length > SHADER_NAMES.length) {
			Log.e(TAG,"Number of shader sources is bigger than number of shaders");
			return -1;
		}

		int shaderProgram = GLES20.glCreateProgram();
		if (shaderProgram == 0) {
			Log.e(TAG,"Unable create new shader program ");
			return -1;
		}
		Log.i(TAG,"New shader program '" + shaderProgram + "' created");

		int[] shaders = new int[shaderSrcArray.length];
		for (int i = 0; i < shaderSrcArray.length; i++) {
			shaders[i] = 0;
			if (shaderSrcArray[i] == null)
				continue;
			Log.i(TAG,"  " + SHADER_FILE_EXTENSIONS[i].substring(1).toUpperCase() + " shader: Creating ... ");
			if (OGLUtils.getVersionGLSL(maxGlEsVersion) < SHADER_SUPPORT_EXTENSIONS[i]) {
				Log.e(TAG, SHADER_NAMES[i] + " shader extension is not supported by OpenGL driver ("
						+ OGLUtils.getVersionGLSL(maxGlEsVersion) + ").");
				continue;
			}
			shaders[i] = createShaderProgram( shaderSrcArray[i], SHADER_NAME_CONSTANTS[i]);
			if (shaders[i] > 0) {
				Log.i(TAG,"'" + shaders[i] + "' OK,  ");
			} else {
				Log.e(TAG,"Shader is not supported");
				continue;
			}

			Log.i(TAG,"Compiling '" + shaders[i] + "'... ");
			shaders[i] = compileShaderProgram( shaders[i]);
			if (shaders[i] > 0) {
				Log.i(TAG,"OK, ");
			} else {

				// Don't leak shaders either
				GLES20.glDeleteShader(shaders[i]);
				shaders[i] = 0;
				return (-1);
			}

			Log.i(TAG,"Attaching '" + shaders[i] + "' to '" + shaderProgram + "' ... ");
			GLES20.glAttachShader(shaderProgram, shaders[i]);
			Log.i(TAG,"OK, ");
		}

		Log.i(TAG,"  Linking shader program '" + shaderProgram + "' ... ");
		if (linkProgram(shaderProgram)) {
			Log.i(TAG,"OK");
		} else {
			// We don't need the program anymore
			GLES20.glDeleteProgram(shaderProgram);
			shaderProgram = 0;
		}

		for (int shader : shaders) {
			if (shader > 0) {
				// Always detach shaders after a successful link
				GLES20.glDetachShader(shaderProgram, shader);
				// Don't leak shader either
				GLES20.glDeleteShader(shader);
			}
		}

		return shaderProgram;
	}

	/**
	 * Read shader code as stream from file
	 * At the end of a
	 * String of code line is char \n added. Chars after // are deleted.
	 *
	 * @param streamFileName
	 *            full path name to a shader file
	 * @return String with GLSL shader code
	 */
	public static String readShaderProgram(Context context, String streamFileName) {

		InputStream is = null;
		try {
			is = context.getAssets().open(streamFileName);
		} catch (IOException e) {
			Log.e(TAG,"File not found ");
			e.printStackTrace();
			return null;
		}
		if (is == null) {
			Log.e(TAG,"File not found ");
			return null;
		}

		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

		String line;
		final StringBuilder shader = new StringBuilder();
		try {
			while ((line = bufferedReader.readLine()) != null) {

				int index = line.indexOf("//");
				if (index > 0)
					line = line.substring(0, index);
				index = line.indexOf("\n");
				if (index < 0)
					line = line + "\n";
				shader.append(line);
			}
			is.close();
			bufferedReader.close();
		} catch (IOException e) {
			Log.e(TAG,"Read error in ");
			e.printStackTrace();
		}

		return shader.toString();
	}

	/**
	 * Create shader and define source String.
	 *
	 * @param shaderSrc
	 *            String with GLSL shader code
	 * @param type
	 *            of shader
	 * @return new id of shader
	 */
	static public int createShaderProgram(String shaderSrc, int type) {
		int shader = GLES20.glCreateShader(type);
		if (shader == 0) {
			return shader;
		}

		GLES20.glShaderSource(shader, shaderSrc);

		return shader;
	}

	/**
	 * Compile shader
	 *
	 * @param shader
	 *            id of shader
	 * @return new id of shader
	 */
	static public int compileShaderProgram( int shader) {
		String error;

		GLES20.glCompileShader(shader);
		error = checkLogInfo( shader, GLES20.GL_COMPILE_STATUS);
		if (error == null) {
			return shader;
		} else {
			Log.e(TAG,"failed");
			Log.e(TAG,"\n" + error);
			if (shader > 0)
				GLES20.glDeleteShader(shader);
			return -1;
		}

	}

	/**
	 * Link shader program
	 *
	 * @param shaderProgram
	 *            id of shader program
	 * @return new id of shader program
	 */
	static public boolean linkProgram( int shaderProgram) {
		String error;
		GLES20.glLinkProgram(shaderProgram);
		error = checkLogInfo( shaderProgram, GLES20.GL_LINK_STATUS);
		if (error == null) {
			return true;
		} else {
			Log.e(TAG,"failed");
			Log.e(TAG,"\n" + error);
			return false;
		}
	}

	static private String checkLogInfo(int programObject, int mode) {
		switch (mode) {
		case GLES20.GL_COMPILE_STATUS:
			return checkLogInfoShader( programObject, mode);
		case GLES20.GL_LINK_STATUS:
		case GLES20.GL_VALIDATE_STATUS:
			return checkLogInfoProgram(programObject, mode);
		default:
			return "Unsupported mode.";
		}
	}

	static private String checkLogInfoShader(int programObject, int mode) {
		int[] error = new int[] { -1 };
		GLES20.glGetShaderiv(programObject, mode, error, 0);
		if (error[0] != GLES20.GL_TRUE) {
			int[] len = new int[1];
			GLES20.glGetShaderiv(programObject, GLES20.GL_INFO_LOG_LENGTH, len, 0);
			if (len[0] == 0) {
				return null;
			}

			return GLES20.glGetShaderInfoLog(programObject);
		}
		return null;
	}

	static private String checkLogInfoProgram(int programObject, int mode) {
		int[] error = new int[] { -1 };
		GLES20.glGetProgramiv(programObject, mode, error, 0);
		if (error[0] != GLES20.GL_TRUE) {
			int[] len = new int[1];
			GLES20.glGetProgramiv(programObject, GLES20.GL_INFO_LOG_LENGTH, len, 0);
			if (len[0] == 0) {
				return null;
			}

			return GLES20.glGetProgramInfoLog(programObject);
		}
		return null;
	}

}
