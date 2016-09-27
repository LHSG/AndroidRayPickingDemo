package com.lhsg.raypickingdemo;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SquareRenderer implements Renderer {

	int viewWidth, viewHeight;

	private float[] mVMatrix = new float[16];
	private float[] mProjMatrix = new float[16];

	Square mSquare;

	@Override
	public void onDrawFrame(GL10 unused) {

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT );


	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		viewWidth = width;
		viewHeight = height;
		
		GLES20.glViewport(0,0,width,height);
		float ratio = (float) width/height;
		//this projection matrix is applied to object coodinates
		//in the onDrawFrame() method
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 10);
		Matrix.setLookAtM(mVMatrix, 0, 0, 0, 3, 0f, 0f, -1.0f, 0f, 1.0f, 0.0f);

		mSquare.draw(mProjMatrix, mVMatrix);
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);

		mSquare = new Square();
	}

	public void handleTouch(float rx, float ry) {
		float [] near_xyz = unProject(rx, ry, 0, mSquare.mMVPMatrix, viewWidth, viewHeight);
		float [] far_xyz = unProject(rx, ry, 1, mSquare.mMVPMatrix, viewWidth, viewHeight);
		//Log.w("가까운 평면상의 좌표", "x:"+near_xyz[0]+", y:"+near_xyz[1]+", z:"+near_xyz[2]);
		//Log.w("먼 평면상의 좌표", "x:"+far_xyz[0]+", y:"+far_xyz[1]+", z:"+far_xyz[2]);
		// 벡터 뺄셈으로
		float[] vec3 = { far_xyz[0]-near_xyz[0], far_xyz[1]-near_xyz[1], far_xyz[2]-near_xyz[2] };
		Log.w("Picking Ray", "x:"+vec3[0]+", y:"+vec3[1]+", z:"+vec3[2]);
		// 위와 같이 산출된 벡터가 월드좌표상의 오브젝트와 교차하는지 판단하면
		// 오브젝트의 선택여부를 확인할 수 있다.
		Vector3D rayVector = new Vector3D( vec3[0], vec3[1], vec3[2] );
		float rayLength = rayVector.length();
		rayVector.normalize();
		Vector3D testPoint = new Vector3D();
		Log.w("���� ��ġ", mSquare.mMMatrix[12]+", "+mSquare.mMMatrix[13]+", "+mSquare.mMMatrix[14]);
		Vector3D modelPoint = new Vector3D(mSquare.mMMatrix[12], mSquare.mMMatrix[13], mSquare.mMMatrix[14]);
		float dist = 0f;
		for(int i=0; i<100;i++) {
			//testPoint.x = rayVector.x * rayLength / 100 * i;
			//testPoint.y = rayVector.y * rayLength / 100 * i;
			//testPoint.z = rayVector.z * rayLength / 100 * i;
			testPoint = rayVector.mul(rayLength/100*i);
			dist = testPoint.sub(modelPoint).length();
			Log.w("�Ÿ�", ""+dist);
			//if(dist<0.2f) Log.w("���ÿ���", "���õ�");
		}
	}

	private  float[] unProject( float winx, float winy, float winz,
								float[] mvpMatrix,
								int width, int height) {
		float[] m = new float[16];
		float[] in = new float[4];
		float[] out = new float[4];

		Matrix.invertM(m, 0, mvpMatrix, 0);

		in[0] = (winx / (float)width) * 2 - 1;
		in[1] = (winy / (float)height) * 2 - 1;
		in[2] = 2 * winz - 1;
		in[3] = 1;

		Matrix.multiplyMV(out, 0, m, 0, in, 0);

		if (out[3]==0)  return null;

		out[3] = 1/out[3];
		return new float[] {out[0] * out[3], out[1] * out[3], out[2] * out[3]};
	}
}