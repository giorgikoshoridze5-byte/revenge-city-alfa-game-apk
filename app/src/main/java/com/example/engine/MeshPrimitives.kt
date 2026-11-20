package com.example.engine

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class VboMesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / 6
    private val vboId: Int

    init {
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        vboId = ids[0]

        val buf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buf.put(vertices).position(0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.size * 4, buf, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun bind(aPos: Int, aNorm: Int) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 24, 0)
        GLES20.glEnableVertexAttribArray(aNorm)
        GLES20.glVertexAttribPointer(aNorm, 3, GLES20.GL_FLOAT, false, 24, 12)
    }

    fun draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }

    fun release() {
        GLES20.glDeleteBuffers(1, intArrayOf(vboId), 0)
    }
}

class MeshPrimitives {
    val cube: VboMesh = VboMesh(buildCube())
    val cylinder: VboMesh = VboMesh(buildCylinder(14))
    val dome: VboMesh = VboMesh(buildSphere(10, 6))

    fun release() {
        cube.release()
        cylinder.release()
        dome.release()
    }

    private fun buildCube(): FloatArray {
        val p = arrayOf(
            // Front (+Z)
            floatArrayOf(-0.5f, -0.5f, 0.5f, 0f, 0f, 1f),
            floatArrayOf(0.5f, -0.5f, 0.5f, 0f, 0f, 1f),
            floatArrayOf(0.5f, 0.5f, 0.5f, 0f, 0f, 1f),
            floatArrayOf(-0.5f, -0.5f, 0.5f, 0f, 0f, 1f),
            floatArrayOf(0.5f, 0.5f, 0.5f, 0f, 0f, 1f),
            floatArrayOf(-0.5f, 0.5f, 0.5f, 0f, 0f, 1f),
            // Back (-Z)
            floatArrayOf(0.5f, -0.5f, -0.5f, 0f, 0f, -1f),
            floatArrayOf(-0.5f, -0.5f, -0.5f, 0f, 0f, -1f),
            floatArrayOf(-0.5f, 0.5f, -0.5f, 0f, 0f, -1f),
            floatArrayOf(0.5f, -0.5f, -0.5f, 0f, 0f, -1f),
            floatArrayOf(-0.5f, 0.5f, -0.5f, 0f, 0f, -1f),
            floatArrayOf(0.5f, 0.5f, -0.5f, 0f, 0f, -1f),
            // Right (+X)
            floatArrayOf(0.5f, -0.5f, 0.5f, 1f, 0f, 0f),
            floatArrayOf(0.5f, -0.5f, -0.5f, 1f, 0f, 0f),
            floatArrayOf(0.5f, 0.5f, -0.5f, 1f, 0f, 0f),
            floatArrayOf(0.5f, -0.5f, 0.5f, 1f, 0f, 0f),
            floatArrayOf(0.5f, 0.5f, -0.5f, 1f, 0f, 0f),
            floatArrayOf(0.5f, 0.5f, 0.5f, 1f, 0f, 0f),
            // Left (-X)
            floatArrayOf(-0.5f, -0.5f, -0.5f, -1f, 0f, 0f),
            floatArrayOf(-0.5f, -0.5f, 0.5f, -1f, 0f, 0f),
            floatArrayOf(-0.5f, 0.5f, 0.5f, -1f, 0f, 0f),
            floatArrayOf(-0.5f, -0.5f, -0.5f, -1f, 0f, 0f),
            floatArrayOf(-0.5f, 0.5f, 0.5f, -1f, 0f, 0f),
            floatArrayOf(-0.5f, 0.5f, -0.5f, -1f, 0f, 0f),
            // Top (+Y)
            floatArrayOf(-0.5f, 0.5f, 0.5f, 0f, 1f, 0f),
            floatArrayOf(0.5f, 0.5f, 0.5f, 0f, 1f, 0f),
            floatArrayOf(0.5f, 0.5f, -0.5f, 0f, 1f, 0f),
            floatArrayOf(-0.5f, 0.5f, 0.5f, 0f, 1f, 0f),
            floatArrayOf(0.5f, 0.5f, -0.5f, 0f, 1f, 0f),
            floatArrayOf(-0.5f, 0.5f, -0.5f, 0f, 1f, 0f),
            // Bottom (-Y)
            floatArrayOf(-0.5f, -0.5f, -0.5f, 0f, -1f, 0f),
            floatArrayOf(0.5f, -0.5f, -0.5f, 0f, -1f, 0f),
            floatArrayOf(0.5f, -0.5f, 0.5f, 0f, -1f, 0f),
            floatArrayOf(-0.5f, -0.5f, -0.5f, 0f, -1f, 0f),
            floatArrayOf(0.5f, -0.5f, 0.5f, 0f, -1f, 0f),
            floatArrayOf(-0.5f, -0.5f, 0.5f, 0f, -1f, 0f)
        )
        val out = FloatArray(p.size * 6)
        var idx = 0
        for (v in p) {
            for (f in v) out[idx++] = f
        }
        return out
    }

    private fun buildCylinder(segments: Int): FloatArray {
        val list = ArrayList<Float>(segments * 12 * 6)
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
            list.add(x); list.add(y); list.add(z)
            list.add(nx); list.add(ny); list.add(nz)
        }
        for (i in 0 until segments) {
            val a0 = (2.0 * PI * i / segments).toFloat()
            val a1 = (2.0 * PI * (i + 1) / segments).toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val x0 = c0 * 0.5f; val z0 = s0 * 0.5f
            val x1 = c1 * 0.5f; val z1 = s1 * 0.5f
            // Side (CCW outward)
            push(x0, -0.5f, z0, c0, 0f, s0)
            push(x0, 0.5f, z0, c0, 0f, s0)
            push(x1, 0.5f, z1, c1, 0f, s1)
            push(x0, -0.5f, z0, c0, 0f, s0)
            push(x1, 0.5f, z1, c1, 0f, s1)
            push(x1, -0.5f, z1, c1, 0f, s1)
            // Top cap (CCW looking from +Y)
            push(0f, 0.5f, 0f, 0f, 1f, 0f)
            push(x1, 0.5f, z1, 0f, 1f, 0f)
            push(x0, 0.5f, z0, 0f, 1f, 0f)
            // Bottom cap (CCW looking from -Y)
            push(0f, -0.5f, 0f, 0f, -1f, 0f)
            push(x0, -0.5f, z0, 0f, -1f, 0f)
            push(x1, -0.5f, z1, 0f, -1f, 0f)
        }
        return list.toFloatArray()
    }

    private fun buildSphere(slices: Int, stacks: Int): FloatArray {
        val list = ArrayList<Float>(slices * stacks * 6 * 6)
        fun push(nx: Float, ny: Float, nz: Float) {
            list.add(nx * 0.5f); list.add(ny * 0.5f); list.add(nz * 0.5f)
            list.add(nx); list.add(ny); list.add(nz)
        }
        for (st in 0 until stacks) {
            val phi0 = (PI * st / stacks - PI * 0.5).toFloat()
            val phi1 = (PI * (st + 1) / stacks - PI * 0.5).toFloat()
            for (sl in 0 until slices) {
                val th0 = (2.0 * PI * sl / slices).toFloat()
                val th1 = (2.0 * PI * (sl + 1) / slices).toFloat()

                val x00 = cos(phi0) * cos(th0); val y00 = sin(phi0); val z00 = cos(phi0) * sin(th0)
                val x10 = cos(phi0) * cos(th1); val y10 = sin(phi0); val z10 = cos(phi0) * sin(th1)
                val x01 = cos(phi1) * cos(th0); val y01 = sin(phi1); val z01 = cos(phi1) * sin(th0)
                val x11 = cos(phi1) * cos(th1); val y11 = sin(phi1); val z11 = cos(phi1) * sin(th1)

                // CCW outward winding
                push(x00, y00, z00); push(x01, y01, z01); push(x11, y11, z11)
                push(x00, y00, z00); push(x11, y11, z11); push(x10, y10, z10)
            }
        }
        return list.toFloatArray()
    }
}
