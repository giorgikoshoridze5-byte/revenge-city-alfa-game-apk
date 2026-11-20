package com.example.engine

import android.opengl.GLES20

class GlShaderProgram {
    val programId: Int
    val aPosition: Int
    val aNormal: Int

    val uMvpMatrix: Int
    val uModelMatrix: Int
    val uColor: Int
    val uEmissive: Int
    val uShininess: Int
    val uSpecStrength: Int
    val uWindowMode: Int
    val uNightFactor: Int
    val uSunDir: Int
    val uSunColor: Int
    val uAmbientSky: Int
    val uAmbientGround: Int
    val uCameraPos: Int
    val uFogColor: Int
    val uFogStart: Int
    val uFogEnd: Int

    init {
        val vertexShaderSrc = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fragmentShaderSrc = """
            precision mediump float;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;

            uniform vec3 uColor;
            uniform float uEmissive;
            uniform float uShininess;
            uniform float uSpecStrength;
            uniform float uWindowMode;
            uniform float uNightFactor;

            uniform vec3 uSunDir;
            uniform vec3 uSunColor;
            uniform vec3 uAmbientSky;
            uniform vec3 uAmbientGround;
            uniform vec3 uCameraPos;
            uniform vec3 uFogColor;
            uniform float uFogStart;
            uniform float uFogEnd;

            void main() {
                vec3 N = normalize(vWorldNormal);
                vec3 L = normalize(uSunDir);
                vec3 V = normalize(uCameraPos - vWorldPos);

                float ndotl = max(dot(N, L), 0.0);
                float hemi = N.y * 0.5 + 0.5;
                vec3 ambient = mix(uAmbientGround, uAmbientSky, hemi);

                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), uShininess) * uSpecStrength * ndotl;
                float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.2) * (0.14 + uSpecStrength * 0.35);

                vec3 baseColor = uColor;
                float emissiveBoost = uEmissive;

                // Procedural skyscraper & apartment window grid on vertical walls
                if (uWindowMode > 0.5 && abs(N.y) < 0.5) {
                    float floorY = fract(vWorldPos.y * 0.34);
                    float colX = fract((vWorldPos.x + vWorldPos.z) * 0.42);
                    float isWindow = step(0.24, floorY) * step(floorY, 0.78) * step(0.22, colX) * step(colX, 0.78);
                    float cellHash = fract(sin(floor(vWorldPos.y * 0.34) * 12.9898 + floor((vWorldPos.x + vWorldPos.z) * 0.42) * 78.233) * 43758.5453);
                    float litChance = step(0.42, cellHash);

                    vec3 glassTint = mix(vec3(0.08, 0.14, 0.22), vec3(0.95, 0.78, 0.42), step(0.75, cellHash));
                    vec3 dayGlass = mix(baseColor, vec3(0.28, 0.48, 0.68), 0.68 * isWindow);
                    vec3 nightLit = mix(vec3(0.05, 0.07, 0.12), glassTint, litChance);
                    baseColor = mix(dayGlass, nightLit, uNightFactor * isWindow);
                    emissiveBoost = max(emissiveBoost, isWindow * litChance * uNightFactor * 0.88);
                }

                vec3 lit = baseColor * (ambient + uSunColor * ndotl * 1.08) + uSunColor * spec + uAmbientSky * fresnel;
                lit = mix(lit, baseColor * 1.18, clamp(emissiveBoost, 0.0, 1.0));

                float dist = length(uCameraPos - vWorldPos);
                float fogFactor = clamp((dist - uFogStart) / max(1.0, (uFogEnd - uFogStart)), 0.0, 1.0);
                vec3 finalColor = mix(lit, uFogColor, fogFactor);

                gl_FragColor = vec4(finalColor, 1.0);
            }
        """.trimIndent()

        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSrc)
        programId = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)

        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormal = GLES20.glGetAttribLocation(programId, "aNormal")

        uMvpMatrix = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
        uModelMatrix = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uColor = GLES20.glGetUniformLocation(programId, "uColor")
        uEmissive = GLES20.glGetUniformLocation(programId, "uEmissive")
        uShininess = GLES20.glGetUniformLocation(programId, "uShininess")
        uSpecStrength = GLES20.glGetUniformLocation(programId, "uSpecStrength")
        uWindowMode = GLES20.glGetUniformLocation(programId, "uWindowMode")
        uNightFactor = GLES20.glGetUniformLocation(programId, "uNightFactor")
        uSunDir = GLES20.glGetUniformLocation(programId, "uSunDir")
        uSunColor = GLES20.glGetUniformLocation(programId, "uSunColor")
        uAmbientSky = GLES20.glGetUniformLocation(programId, "uAmbientSky")
        uAmbientGround = GLES20.glGetUniformLocation(programId, "uAmbientGround")
        uCameraPos = GLES20.glGetUniformLocation(programId, "uCameraPos")
        uFogColor = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogStart = GLES20.glGetUniformLocation(programId, "uFogStart")
        uFogEnd = GLES20.glGetUniformLocation(programId, "uFogEnd")
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }
}
