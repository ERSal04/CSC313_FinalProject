#version 410 core

layout(location = 0) in vec3 position;

uniform float time;        // current time in seconds — animates the waves
uniform float amplitude;   // wave height
uniform float frequency;   // how many waves fit across the grid
uniform float speed;        // how fast waves move
uniform vec2  direction;   // which direction waves travel (normalized)

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec3 fragNormal;

vec3 gerstner(vec3 pos, vec2 dir, float amp, float freq, float spd, float t) {
    float phase = dot(dir, pos.xz) * freq - t * spd;
    float x = amp * cos(phase) * dir.x;
    float y = amp * sin(phase);
    float z = amp * cos(phase) * dir.y;
    return vec3(x, y, z);
}

void main() {
    vec3 pos = position;

    pos += gerstner(pos, vec2(1.0, 0.0), amplitude, frequency, speed, time);
    pos += gerstner(pos, vec2(0.8, 0.6), amplitude * 0.5, frequency * 1.5, speed * 0.9, time);
    pos += gerstner(pos, vec2(-0.5, 0.8), amplitude * 0.3, frequency * 2.0, speed * 1.2, time);

    fragNormal = vec3(0.0, 1.0, 0.0);

    gl_Position = projection * view * model * vec4(pos, 1.0);
}