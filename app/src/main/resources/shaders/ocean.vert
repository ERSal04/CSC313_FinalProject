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

uniform int   tsunamiActive;   // 0 or 1
uniform vec2  tsunamiOrigin;   // XZ world position of the earthquake
uniform float tsunamiTime;     // seconds since tsunami was triggered

out vec3 fragNormal;
out vec3 fragPos;

vec3 gerstner(vec3 pos, vec2 dir, float amp, float freq, float spd, float t) {
    float phase = dot(dir, pos.xz) * freq - t * spd;
    float x = amp * cos(phase) * dir.x;
    float y = amp * sin(phase);
    float z = amp * cos(phase) * dir.y;
    return vec3(x, y, z);
}

void main() {
    vec3 pos = position;

    pos += gerstner(pos, vec2(1.0, 0.0),  amplitude,       frequency,       speed,       time);
    pos += gerstner(pos, vec2(0.8, 0.6),  amplitude * 0.5, frequency * 1.5, speed * 0.9, time);
    pos += gerstner(pos, vec2(-0.5, 0.8), amplitude * 0.3, frequency * 2.0, speed * 1.2, time);

    // Tsunami wave — circular ripple from origin point
    if (tsunamiActive == 1) {
        float dist       = length(position.xz - tsunamiOrigin);
        float waveSpeed  = 40.0;  // units per second, tsunamis travel fast
        float waveFront  = tsunamiTime * waveSpeed;
        float waveWidth  = 30.0;  // how wide the wave wall is

        // The wave is a travelling wall — peak at waveFront, falls off either side
        float distFromFront = dist - waveFront;
        float envelope = exp(-distFromFront * distFromFront / (waveWidth * waveWidth));

        // Height scales with distance — real tsunamis grow as water shallows
        float tsunamiHeight = 8.0 * envelope;

        pos.y += tsunamiHeight;
    }

    // Normal calculation stays the same
    fragNormal = vec3(0.0, 1.0, 0.0);
    fragPos    = vec3(model * vec4(pos, 1.0));

    gl_Position = projection * view * model * vec4(pos, 1.0);
}