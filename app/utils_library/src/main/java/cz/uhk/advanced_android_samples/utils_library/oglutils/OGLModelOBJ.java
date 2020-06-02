package cz.uhk.advanced_android_samples.utils_library.oglutils;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class OGLModelOBJ {
	private int topology;
	private static final String TAG = "OGLModelOBJ";
	private OGLBuffers buffer;
	
	public OGLBuffers getBuffers() {
		return buffer;
	}

	public int getTopology() {
		return topology;
	}

/*
	private List<Integer> geometryList;
	
	private List<OGLBuffers> bufferList;
	
	public List<OGLBuffers> getBufferList() {
		return bufferList;
	}

	public List<Integer> getGeometryList() {
		return geometryList;
	}
*/
	public OGLModelOBJ(Context context, String modelPath) {

		class OBJLoader{
			private List<float[]> vData = new ArrayList<>(); // List of Vertex Coordinates
			private List<float[]> vtData = new ArrayList<>(); // List of Texture Coordinates
			private List<float[]> vnData = new ArrayList<>(); // List of Normal Coordinates
			private List<int[]> fv = new ArrayList<>(); // Face Vertex Indices;
			private List<int[]> ft = new ArrayList<>(); // Face Texture Indices
			private List<int[]> fn = new ArrayList<>(); // Face Normal Indices

			private OBJLoader(String modelPath) {
				loadOBJModel(modelPath);
				setFaceRenderType();
			}

			private void loadOBJModel(String modelPath) {
				try {
					// Open a file handle and read the models data
					InputStream is = context.getAssets().open(modelPath);

					if (is == null)
						return;

					BufferedReader br = null;
					try {
						br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						Log.e(TAG, "File not found ");
						e.printStackTrace();
						return;
					}
					String line = null;
					while ((line = br.readLine()) != null) {
						if (line.startsWith("#")) {
						} else if (line.equals("")) {
							// Ignore whitespace data
						} else if (line.startsWith("v ")) { 
							// Read in Vertex Data
							vData.add(processData(line));
						} else if (line.startsWith("vt ")) { 
							// Read Texture Coordinates
							vtData.add(processData(line));
						} else if (line.startsWith("vn ")) { 
							// Read Normal Coordinates
							vnData.add(processData(line));
						} else if (line.startsWith("f ")) { 
							// Read Face (index) Data
							processFaceData(line);
						}
					}
					is.close();
					br.close();
					Log.i(TAG,"OBJ model: " + modelPath + "... read");
				} catch (IOException e) {
					Log.i(TAG,"Failed to find or read OBJ: " + modelPath);
					Log.e(TAG,e.toString());
				}
			}

			private float[] processData(String read) {
				String s[] = read.split("\\s+");
				return (processFloatData(s)); 
			}

			private float[] processFloatData(String sdata[]) {
				float data[] = new float[sdata.length - 1];
				for (int loop = 0; loop < data.length; loop++) {
					data[loop] = Float.parseFloat(sdata[loop + 1]);
				}
				return data; 
			}

			private void processFaceData(String fread) {
				String s[] = fread.split("\\s+");
				if (fread.contains("//")) { 
					// Pattern is present if obj has only v and vn in face data
					for (int loop = 1; loop < s.length; loop++) {
						s[loop] = s[loop].replaceAll("//", "/1/"); 
						// insert a zero for missing vt data
					}
				}
				processfIntData(s); // Pass in face data
			}

			private void processfIntData(String sdata[]) {

				int[] vdata = new int[3];
				int[] vtdata = new int[3];
				int[] vndata = new int[3];

				for (int loop = 1; loop < sdata.length; loop++) {
					String s = sdata[loop];
					String[] temp = s.split("/");
					int index = loop - 1;
					if (loop>3) {	//make a new triangle as a triangle fan
						fv.add(vdata);		//save previous triangle
						ft.add(vtdata);
						fn.add(vndata);

						int[] vdataN = new int[3];
						int[] vtdataN = new int[3];
						int[] vndataN = new int[3];

						vdataN[0] = vdata[0]; //first vertex always at index 0
						vtdataN[0] = vtdata[0];
						vndataN[0] = vndata[0];
						
						vdataN[1] = vdata[2]; //second vertex is the third one of previous triangle 
						vtdataN[1] = vtdata[2];
						vndataN[1] = vndata[2];
						index = 2;
						
						vdata = vdataN; 
						vtdata = vtdataN;
						vndata = vndataN;
					}
					
					vdata[index] = Integer.valueOf(temp[0]);
					// always add vertex indices

					if (temp.length > 1) {// if true, we have v and vt data
						vtdata[index] = Integer.valueOf(temp[1]);
						// add in vt indices
					} else {
						vtdata[index] = 0; // if no vt data is present fill in zeros
					}
					if (temp.length > 2) {// if true, we have v, vt, and vn data
						vndata[index] = Integer.valueOf(temp[2]);
						// add in vn indices
					} else {
						vndata[index] = 0;// if no vn data is present fill in zeros
					}
				}
				fv.add(vdata);
				ft.add(vtdata);
				fn.add(vndata);
			}

			private void setFaceRenderType() {
				topology = GLES20.GL_TRIANGLES;
			}

		}
		
		FloatBuffer tmpVerticesBuf = null, tmpNormalsBuf=null, tmpTexCoordsBuf= null;
		
		OBJLoader loader = new OBJLoader(modelPath); 
		
		float coords4[] = new float[4];
		Log.i(TAG,loader.fv.size() + " " + (loader.fv.get(0)).length);
		if (loader.fv.get(0)[0] > 0) {
			tmpVerticesBuf = ByteBuffer.allocateDirect(loader.fv.size() * 4 * 4
					* (loader.fv.get(0)).length).order(ByteOrder.nativeOrder()).asFloatBuffer();
			tmpVerticesBuf.position(0);

			coords4[3] = 1;
			for (int i = 0; i < loader.fv.size(); i++) {
				for (int j = 0; j < (loader.fv.get(i)).length; j++) {
					coords4[0] = loader.vData.get(loader.fv.get(i)[j] - 1)[0]; // x
					coords4[1] = loader.vData.get(loader.fv.get(i)[j] - 1)[1]; // y
					coords4[2] = loader.vData.get(loader.fv.get(i)[j] - 1)[2]; // z
					tmpVerticesBuf.put(coords4);
				}

			}
			tmpVerticesBuf.position(0);
		}

		if (loader.ft.get(0)[0] > 0) {
			tmpTexCoordsBuf = ByteBuffer.allocateDirect(loader.ft.size() * 2 * 4
					* (loader.ft.get(0)).length).order(ByteOrder.nativeOrder()).asFloatBuffer();
			tmpTexCoordsBuf.position(0);

			for (int i = 0; i < loader.ft.size(); i++) {
				try {
					
		//			for (int j = 0; j < 3; j++) {
					for (int j = 0; j < ( loader.ft.get(i)).length; j++) {
								tmpTexCoordsBuf
							.put( loader.vtData.get(loader.ft.get(i)[j] - 1)[0]);
					tmpTexCoordsBuf
							.put( loader.vtData.get(loader.ft.get(i)[j] - 1)[1]);
				}
				} catch (ArrayIndexOutOfBoundsException exception) {
					Log.i(TAG,i + " " + (loader.ft.get(i)).length);
					Log.i(TAG, loader.ft.get(i) + "");
					Log.i(TAG,loader.ft.get(i)[0] + " " + loader.ft.get(i)[1] + " "+loader.ft.get(i)[2] + "");
					Log.i(TAG,loader.vtData.get(loader.ft.get(i)[0] - 1)+ "");
					Log.i(TAG,loader.vtData.get(loader.ft.get(i)[1] - 1)+ "");
					Log.i(TAG,loader.vtData.get(loader.ft.get(i)[2] - 1)+ "");
					exception.printStackTrace();
					return;
				}
				
			}
			tmpTexCoordsBuf.position(0);
		}

		float coords3[] = new float[3];
		if (loader.fn.get(0)[0] > 0) {
			tmpNormalsBuf = ByteBuffer.allocateDirect(loader.fn.size() * 3 * 4
					* (loader.fn.get(0)).length).order(ByteOrder.nativeOrder()).asFloatBuffer();
			tmpNormalsBuf.position(0);

			for (int i = 0; i < loader.fn.size(); i++) {
				for (int j = 0; j < ( loader.fn.get(i)).length; j++) {
					coords3[0] = loader.vnData.get(loader.fn.get(i)[j] - 1)[0]; // x
					coords3[1] = loader.vnData.get(loader.fn.get(i)[j] - 1)[1]; // y
					coords3[2] = loader.vnData.get(loader.fn.get(i)[j] - 1)[2]; // z
					tmpNormalsBuf.put(coords3);
				}
			}
			tmpNormalsBuf.position(0);
		}
		
		buffer = toOGLBuffers(tmpVerticesBuf, tmpNormalsBuf, tmpTexCoordsBuf);
	}

	
	private OGLBuffers toOGLBuffers(FloatBuffer verticesBuf, FloatBuffer normalsBuf, FloatBuffer texCoordsBuf){
		OGLBuffers buffers;
		
		if (verticesBuf != null) {
			OGLBuffers.Attrib[] attributesPos = {
					new OGLBuffers.Attrib("inPosition", 4),
			};
			float[] floatArray = new float[verticesBuf.limit()];
			verticesBuf.get(floatArray);
	        buffers = new OGLBuffers(floatArray, attributesPos, null);
		}
		else
			return null;

		if (texCoordsBuf != null) {
			OGLBuffers.Attrib[] attributesTexCoord = {
					new OGLBuffers.Attrib("inTexCoord", 2)
			};
			float[] floatArray = new float[texCoordsBuf.limit()];
			texCoordsBuf.get(floatArray);
			buffers.addVertexBuffer(floatArray, attributesTexCoord);
		}
			
		if (normalsBuf != null) {
			OGLBuffers.Attrib[] attributesNormal = {
					new OGLBuffers.Attrib("inNormal", 3)
			};
			float[] floatArray = new float[normalsBuf.limit()];
			normalsBuf.get(floatArray);
			buffers.addVertexBuffer(floatArray, attributesNormal);
		}
			
		return buffers;
	}

}