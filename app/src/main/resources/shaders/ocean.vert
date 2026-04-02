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

void main() {
    vec3 pos = position;

    // Dot porduct of direction and position
    float k = frequency;
    float phase = dot(direction, pos.xz) * k - time * speed;

    pos.y += amplitude * sin(phase);
    pos.x += amplitude * cos(phase) * direction.x;
    pos.z += amplitude * cos(phase) * direction.y;

    gl_Position = projection * view * model * vec4(pos, 1.0);
}