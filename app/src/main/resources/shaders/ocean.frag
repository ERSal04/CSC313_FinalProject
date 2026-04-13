#version 330 core

in vec3 fragNormal;
in vec3 fragPos;

uniform vec3 sunDirection;
uniform vec3 cameraPos;      // now actually used for specular

out vec4 fragColor;

void main() {
    vec3 norm    = normalize(fragNormal);
    vec3 sunDir  = normalize(sunDirection);
    vec3 viewDir = normalize(cameraPos - fragPos);

    // Diffuse
    float diff = max(dot(norm, sunDir), 0.0);

    // Specular (Blinn-Phong)
    vec3  halfDir = normalize(sunDir + viewDir);
    float spec    = pow(max(dot(norm, halfDir), 0.0), 64.0);

    // Depth-based color: deeper troughs are darker blue
    float depthT   = clamp(fragPos.y * 0.3 + 0.5, 0.0, 1.0);
    vec3  deepColor = vec3(0.0,  0.15, 0.35);
    vec3  surfColor = vec3(0.0,  0.45, 0.75);
    vec3  waterBase = mix(deepColor, surfColor, depthT);

    vec3 ambient  = waterBase * 0.2;
    vec3 diffuse  = waterBase * diff * 0.7;
    vec3 specular = vec3(1.0) * spec * 0.8;

    fragColor = vec4(ambient + diffuse + specular, 1.0);
}