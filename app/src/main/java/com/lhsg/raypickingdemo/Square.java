package com.lhsg.raypickingdemo;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;

import junit.framework.Assert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

/**
 * Created by artilla on 2016. 9. 27..
 */

public class Square {
    float[] vertices;
    private FloatBuffer vbuf;

    float[] mMVPMatrix = new float[16];
    float[] mMMatrix = new float[16];

    private float[] mMVMatrix = new float[16];

    private int mProgram;
    private int maPositionHandle;
    private int muMVPMatrixHandle;

    public Square() {
        initShapes();

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL program executables
        // get handle to the vertex shader's vPosition member
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    }

    public void draw(float[] projMatrix, float[] viewMatrix) {
        //glUseProgram
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        Assert.assertTrue(GLES20.glGetError() == 0);
        // Prepare the square data
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vbuf);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        Assert.assertTrue(GLES20.glGetError() == 0);
        Matrix.setIdentityM(mMMatrix, 0);
//		Matrix.translateM(mMMatrix, 0, 0.5f, 1.5f, -5.0f);
        Matrix.translateM(mMMatrix, 0, 0.0f, 0.0f, -5.0f);
        Matrix.multiplyMM(mMVMatrix, 0, viewMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, projMatrix, 0, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        Assert.assertTrue(GLES20.glGetError() == 0);
        // Draw the square(2개의 삼각형이므로 정점은 6개)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    public void rayPicking(int viewWidth, int viewHeight, float rx, float ry) {

        float [] near_xyz = unProject(rx, ry, 0, mMVPMatrix, viewWidth, viewHeight);
        float [] far_xyz = unProject(rx, ry, 1, mMVPMatrix, viewWidth, viewHeight);

        int coordCount = vertices.length;
        float[] convertedSquare = new float[coordCount];
        float[] resultVector = new float[4];
        float[] inputVector = new float[4];

        for(int i = 0; i < coordCount; i = i + 3){
            inputVector[0] = vertices[i];
            inputVector[1] = vertices[i+1];
            inputVector[2] = vertices[i+2];
            inputVector[3] = 1;
            Matrix.multiplyMV(resultVector, 0, mMVMatrix, 0, inputVector,0);
            convertedSquare[i] = resultVector[0]/resultVector[3];
            convertedSquare[i+1] = resultVector[1]/resultVector[3];
            convertedSquare[i+2] = resultVector[2]/resultVector[3];
        }

        Triangle t1 = new Triangle(new float[] {convertedSquare[0], convertedSquare[1], convertedSquare[2]}, new float[] {convertedSquare[3], convertedSquare[4], convertedSquare[5]}, new float[] {convertedSquare[6], convertedSquare[7], convertedSquare[8]});
        Triangle t2 = new Triangle(new float[] {convertedSquare[9], convertedSquare[10], convertedSquare[11]}, new float[] {convertedSquare[12], convertedSquare[13], convertedSquare[14]}, new float[] {convertedSquare[15], convertedSquare[16], convertedSquare[17]});

        float[] point1 = new float[3];
        int intersects1 = Triangle.intersectRayAndTriangle(near_xyz, far_xyz, t1, point1);
        float[] point2 = new float[3];
        int intersects2 = Triangle.intersectRayAndTriangle(near_xyz, far_xyz, t2, point2);

        if (intersects1 == 1 || intersects1 == 2) {
            Log.d("test", "touch!: ");
        }
        else if (intersects2 == 1 || intersects2 == 2) {
            Log.d("test", "touch!: ");
        }
    }
    private int loadShader(int type, String shaderCode){
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);
        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private void initShapes(){
        vertices = new float[]{
                // X, Y, Z
                -0.5f, 0.5f, 0f,
                -0.5f, -0.5f, 0f,
                0.5f, -0.5f, 0f,

                -0.5f, 0.5f, 0f,
                0.5f, -0.5f, 0f,
                0.5f, 0.5f, 0f
        };
        // initialize vertex Buffer for triangle
        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());	// use the device hardware's native byte order
        vbuf = vbb.asFloatBuffer();	// create a floating point buffer from the ByteBuffer
        vbuf.put(vertices);		// add the coordinates to the FloatBuffer
        vbuf.position(0);		// set the buffer to read the first coordinate
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

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;  \n" +
                    "attribute vec4 aPosition; \n" +
                    "void main(){              \n" +
                    " gl_Position = uMVPMatrix * aPosition; \n" +
                    "}                         \n";

    private final String fragmentShaderCode =
            "precision mediump float;  \n" +
                    "void main(){              \n" +
                    " gl_FragColor = vec4 (0, 0.5, 0, 1.0); \n" +
                    "}                         \n";

}
