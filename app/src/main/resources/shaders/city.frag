#version 330 core

in vec3  fragNormal;
in vec3  fragPos;
in vec2  fragTexCoord;
in float floodHeight;

uniform vec3      sunDirection;
uniform vec3      cameraPos;
uniform sampler2D diffuseTexture;
uniform int       hasTexture;

out vec4 fragColor;

void main() {
    vec3 norm    = normalize(fragNormal);
    vec3 sunDir  = normalize(sunDirection);
    vec3 viewDir = normalize(cameraPos - fragPos);

    float diff = max(dot(norm, sunDir), 0.15);

    vec3 halfDir = normalize(sunDir + viewDir);
    float spec   = pow(max(dot(norm, halfDir), 0.0), 32.0);

    // Use texture if available, otherwise use flat grey
    vec3 baseColor;
    if (hasTexture == 1) {
        baseColor = texture(diffuseTexture, fragTexCoord).rgb;
    } else {
        baseColor = vec3(0.6, 0.6, 0.65);
    }

    // Flood submersion tint
    vec3 floodColor = vec3(0.2, 0.35, 0.25);
    baseColor = mix(baseColor, floodColor, floodHeight);

    vec3 result = baseColor * diff + vec3(0.8) * spec * 0.3;
    fragColor   = vec4(result, 1.0);
}