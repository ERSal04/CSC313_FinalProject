#version 330 core

layout(location = 0) in vec3 position;

uniform float time;
uniform float amplitude;
uniform float frequency;
uniform float speed;
uniform vec2  direction;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform int   tsunamiActive;
uniform vec2  tsunamiOrigin;
uniform float tsunamiTime;

out vec3 fragNormal;
out vec3 fragPos;

vec3 gerstnerDisplace(vec3 pos, vec2 dir, float amp,
                      float freq, float spd, float t) {
    float phase = dot(dir, pos.xz) * freq - t * spd;
    return vec3(
        amp * cos(phase) * dir.x,
        amp * sin(phase),
        amp * cos(phase) * dir.y
    );
}

vec3 gerstnerNormal(vec3 pos, vec2 dir, float amp,
                    float freq, float spd, float t) {
    float phase = dot(dir, pos.xz) * freq - t * spd;
    float cosP  = cos(phase);
    return vec3(
        -dir.x * freq * amp * cosP,
         0.0,
        -dir.y * freq * amp * cosP
    );
}

void main() {
    vec3 pos = position;

    // Shore mask — waves fade to zero as they approach land at z=15
    // Ocean is negative Z, land starts at positive Z around 15
    // smoothstep(15, -30, pos.z):
    //   at z = -30 or less  → mask = 1.0 (full waves)
    //   at z =  15 or more  → mask = 0.0 (flat, no waves)
    float shoreMask = smoothstep(15.0, -30.0, pos.z);

    float safeAmp = amplitude * shoreMask;

    pos += gerstnerDisplace(pos, vec2(1.0,  0.0), safeAmp,
                            frequency, speed, time);
    pos += gerstnerDisplace(pos, vec2(0.8,  0.6), safeAmp * 0.5,
                            frequency * 1.5, speed * 0.9, time);
    pos += gerstnerDisplace(pos, vec2(-0.5, 0.8), safeAmp * 0.3,
                            frequency * 2.0, speed * 1.2, time);

    vec3 n = vec3(0.0, 1.0, 0.0);
    n += gerstnerNormal(position, vec2(1.0,  0.0), safeAmp,
                        frequency, speed, time);
    n += gerstnerNormal(position, vec2(0.8,  0.6), safeAmp * 0.5,
                        frequency * 1.5, speed * 0.9, time);
    n += gerstnerNormal(position, vec2(-0.5, 0.8), safeAmp * 0.3,
                        frequency * 2.0, speed * 1.2, time);

    // Tsunami
    if (tsunamiActive == 1) {
        float dist          = length(position.xz - tsunamiOrigin);
        float waveFront     = tsunamiTime * 40.0;
        float waveWidth     = 30.0;
        float distFromFront = dist - waveFront;
        float envelope      = exp(-(distFromFront * distFromFront)
                                  / (waveWidth * waveWidth));

        // Tsunami also uses shore mask so it doesn't go under city
        float tsunamiHeight = 8.0 * envelope * shoreMask;
        pos.y += tsunamiHeight;

        vec2  toOrigin    = normalize(tsunamiOrigin - position.xz);
        float tsunamiSlope = -2.0 * distFromFront / (waveWidth * waveWidth)
                             * 8.0 * envelope * shoreMask;
        n.x += toOrigin.x * tsunamiSlope;
        n.z += toOrigin.y * tsunamiSlope;
    }

    fragNormal = normalize(n);
    fragPos    = vec3(model * vec4(pos, 1.0));
    gl_Position = projection * view * model * vec4(pos, 1.0);
}