#version 410 core

in vec3 fragNormal;
uniform vec3 sunDirection;

out vec4 fragColor;

void main() {
    float light = max(dot(fragNormal, normalize(sunDirection)), 0.15);
    vec3 waterColor = vec3(0.0, 0.4, 0.7);
    fragColor = vec4(waterColor * light, 1.0);
}