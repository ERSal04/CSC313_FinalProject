#version 330 core

in vec3  fragNormal;
in vec3  fragPos;
in float floodHeight;

uniform vec3 sunDirection;
uniform vec3 cameraPos;

out vec4 fragColor;

void main() {
    vec3 norm    = normalize(fragNormal);
    vec3 sunDir  = normalize(sunDirection);
    vec3 viewDir = normalize(cameraPos - fragPos);

    float diff    = max(dot(norm, sunDir), 0.0);
    float ambient = 0.45;
    float light   = ambient + diff * 0.55;

    vec3 halfDir = normalize(sunDir + viewDir);
    float spec   = pow(max(dot(norm, halfDir), 0.0), 32.0);

    vec3 baseColor;

    // Ground and beach faces point upward — give them terrain color
    if (norm.y > 0.7) {
        // Sandy near coast (low Z), greener inland (high Z)
        vec3 sandColor  = vec3(0.76, 0.70, 0.50);
        vec3 grassColor = vec3(0.40, 0.55, 0.30);
        float grassT    = clamp((fragPos.z - 15.0) / 80.0, 0.0, 1.0);
        baseColor = mix(sandColor, grassColor, grassT);

        // Flood darkens the ground to muddy brown
        vec3 mudColor = vec3(0.30, 0.22, 0.12);
        baseColor = mix(baseColor, mudColor, floodHeight);

    } else {
        // Vertical faces = building walls
        float heightT  = clamp(fragPos.y / 50.0, 0.0, 1.0);
        vec3 lowColor  = vec3(0.50, 0.48, 0.52);
        vec3 highColor = vec3(0.72, 0.74, 0.80);
        baseColor = mix(lowColor, highColor, heightT);

        // Floor band lines
        float floorLine  = abs(mod(fragPos.y, 3.0) - 0.1);
        float windowBand = smoothstep(0.0, 0.3, floorLine);
        baseColor = mix(baseColor * 0.55, baseColor, windowBand);

        // Flood tint on buildings
        vec3 floodColor = vec3(0.20, 0.35, 0.25);
        baseColor = mix(baseColor, floodColor, floodHeight);
    }

    vec3 result = baseColor * light + vec3(0.9) * spec * 0.2;
    fragColor   = vec4(result, 1.0);
}