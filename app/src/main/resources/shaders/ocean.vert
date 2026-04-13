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

    // Approximate normal from wave slope
    float eps = 0.01;
    vec3 posX = position + vec3(eps, 0, 0);
    vec3 posZ = position + vec3(0, 0, eps);
    posX += gerstner(posX, vec2(1.0, 0.0),  amplitude,       frequency,       speed,       time);
    posX += gerstner(posX, vec2(0.8, 0.6),  amplitude * 0.5, frequency * 1.5, speed * 0.9, time);
    posX += gerstner(posX, vec2(-0.5, 0.8), amplitude * 0.3, frequency * 2.0, speed * 1.2, time);
    posZ += gerstner(posZ, vec2(1.0, 0.0),  amplitude,       frequency,       speed,       time);
    posZ += gerstner(posZ, vec2(0.8, 0.6),  amplitude * 0.5, frequency * 1.5, speed * 0.9, time);
    posZ += gerstner(posZ, vec2(-0.5, 0.8), amplitude * 0.3, frequency * 2.0, speed * 1.2, time);

    fragNormal = normalize(cross(posZ - pos, posX - pos));
    fragPos    = vec3(model * vec4(pos, 1.0));

    gl_Position = projection * view * model * vec4(pos, 1.0);
}