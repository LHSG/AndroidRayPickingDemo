package com.lhsg.raypickingdemo;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by artilla on 2016. 9. 27..
 */

public class Square {
    private final static String TAG = "Square";

    float[] vertices;
    private FloatBuffer vbuf;

    float[] mMVPMatrix = new float[16];
    float[] mMMatrix = new float[16];

    private float[] mMVMatrix = new float[16];

    private int mProgram;
    private int maPositionHandle;
    private int muColorHandle;
    private int muMVPMatrixHandle;

    private float[] position;
    private String name;
    float color[];

    public Square(String name, float[] color, float[] position) {
        this.name = name;
        this.color = color;
        this.position = position;

        initShapes();

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL program executables
        // get handle to the vertex shader's vPosition member
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        muColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    }

    public void draw(float[] projMatrix, float[] viewMatrix) {
        GLES20.glUseProgram(mProgram);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vbuf);
        GLES20.glEnableVertexAttribArray(maPositionHandle);

        GLES20.glUniform4fv(muColorHandle, 1, color, 0);

        Matrix.setIdentityM(mMMatrix, 0);
		Matrix.translateM(mMMatrix, 0, position[0], position[1], position[2]);
        Matrix.multiplyMM(mMVMatrix, 0, viewMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, projMatrix, 0, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
    }

    public void rayPicking(int viewWidth, int viewHeight, float rx, float ry, float[] viewMatrix, float[] projMatrix) {

        float [] near_xyz = unProject(rx, ry, 0, viewMatrix, projMatrix, viewWidth, viewHeight);
        float [] far_xyz = unProject(rx, ry, 1, viewMatrix, projMatrix, viewWidth, viewHeight);

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
            Log.d(TAG, "touch!: " + name);
        }
        else if (intersects2 == 1 || intersects2 == 2) {
            Log.d(TAG, "touch!: " + name);
        }
    }
    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
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

    private  float[] unProject( float xTouch, float yTouch, float winz,
                                float[] viewMatrix,
                                float[] projMatrix,
                                int width, int height) {
        int[] viewport = {0, 0, width, height};

        float[] out = new float[3];
        float[] temp = new float[4];
        float[] temp2 = new float[4];
        // get the near and far ords for the click

        float winx = xTouch, winy =(float)viewport[3] - yTouch;

        int result = GLU.gluUnProject(winx, winy, winz, viewMatrix, 0, projMatrix, 0, viewport, 0, temp, 0);

        Matrix.multiplyMV(temp2, 0, viewMatrix, 0, temp, 0);
        if(result == 1){
            out[0] = temp2[0] / temp2[3];
            out[1] = temp2[1] / temp2[3];
            out[2] = temp2[2] / temp2[3];
        }
        return out;
    }

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;  \n" +
                    "attribute vec4 aPosition; \n" +
                    "void main(){              \n" +
                    " gl_Position = uMVPMatrix * aPosition; \n" +
                    "}                         \n";

    private final String fragmentShaderCode =
            "precision mediump float;  \n" +
            "uniform vec4 vColor;      \n" +
            "void main(){              \n" +
            "  gl_FragColor = vColor;  \n" +
            "}                         \n";

}
