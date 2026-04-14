#version 330 core

in vec3  fragNormal;
in vec3  fragPos;
in vec2  fragTexCoord;
in float floodHeight;

uniform vec3      sunDirection;
uniform vec3      moonDirection;   // ADD
uniform vec3      cameraPos;
uniform sampler2D diffuseTexture;
uniform int       hasTexture;
uniform int       isGround;
uniform vec3      skyColor;
uniform float     fogStart;
uniform float     fogEnd;

out vec4 fragColor;

void main() {
    vec3 norm    = normalize(fragNormal);
    vec3 sunDir  = normalize(sunDirection);
    vec3 moonDir = normalize(moonDirection);
    vec3 viewDir = normalize(cameraPos - fragPos);

    // --- Time of day factors from sun height ---
    float sunHeight   = sunDirection.y;
    float dayFactor   = clamp(sunHeight * 2.0 + 0.2, 0.0, 1.0);
    float nightFactor = clamp(1.0 - dayFactor * 2.0, 0.0, 1.0);

    // --- Ambient light ---
    // Daytime: bright warm ambient
    // Nighttime: dim cool moonlit ambient
    vec3 dayAmbient  = vec3(0.45);
    vec3 moonAmbient = vec3(0.06, 0.07, 0.12);  // slightly brighter, cooler blue
    vec3 ambient     = mix(moonAmbient, dayAmbient, dayFactor);

    // --- Sun diffuse ---
    float sunDiff  = max(dot(norm, sunDir), 0.0) * dayFactor;

    // --- Moon diffuse ---
    // Moon provides soft directional light at night
    float moonDiff = max(dot(norm, moonDir), 0.0)
                     * nightFactor * 0.35;
    // Moon light is cool blue-white
    vec3 moonDiffColor = vec3(0.6, 0.7, 0.9);

    // --- Specular ---
    // Sun specular during day
    vec3  sunHalf  = normalize(sunDir  + viewDir);
    float sunSpec  = pow(max(dot(norm, sunHalf),  0.0), 32.0)
                     * dayFactor * 0.3;

    // Moon specular at night — sharp cold glint on glass buildings
    vec3  moonHalf = normalize(moonDir + viewDir);
    float moonSpec = pow(max(dot(norm, moonHalf), 0.0), 64.0)
                     * nightFactor * 0.5;
    vec3  moonSpecColor = vec3(0.7, 0.8, 1.0);

    // --- Base color ---
    vec3 baseColor;

    if (isGround == 1) {
        vec3 sandColor  = vec3(0.76, 0.70, 0.50);
        vec3 grassColor = vec3(0.40, 0.55, 0.30);
        float grassT    = clamp((fragPos.z - 15.0) / 80.0, 0.0, 1.0);
        baseColor       = mix(sandColor, grassColor, grassT);

        vec3 mudColor = vec3(0.30, 0.22, 0.12);
        baseColor     = mix(baseColor, mudColor, floodHeight);

    } else {
        if (hasTexture == 1) {
            baseColor = texture(diffuseTexture, fragTexCoord).rgb;
        } else {
            float heightT  = clamp(fragPos.y / 50.0, 0.0, 1.0);
            vec3 lowColor  = vec3(0.50, 0.48, 0.52);
            vec3 highColor = vec3(0.72, 0.74, 0.80);
            baseColor      = mix(lowColor, highColor, heightT);

            float floorLine  = abs(mod(fragPos.y, 3.0) - 0.1);
            float windowBand = smoothstep(0.0, 0.3, floorLine);
            baseColor        = mix(baseColor * 0.55, baseColor, windowBand);
        }

        vec3 floodColor = vec3(0.20, 0.35, 0.25);
        baseColor       = mix(baseColor, floodColor, floodHeight);
    }

    // --- Windows glow at night ---
    vec3 windowGlow = vec3(0.0);
    if (isGround == 0 && nightFactor > 0.1) {
        // Only show on vertical faces, not rooftops
        float verticalFace = 1.0 - abs(norm.y);

        // Tighter floor bands — each floor is ~3 units, window is thin strip
        float floorY      = mod(fragPos.y, 3.0);
        float windowBand  = smoothstep(0.3, 0.0, abs(floorY - 1.5))   // center strip
                        * smoothstep(0.0, 0.4, floorY);              // soft bottom edge

        // Not every window is lit — use position to pseudo-randomly darken some
        float xGrid   = mod(floor(fragPos.x * 0.5), 2.0);
        float zGrid   = mod(floor(fragPos.z * 0.5), 2.0);
        float litness = mod(xGrid + zGrid + floor(fragPos.y / 3.0), 2.0);
        float occupied = 0.4 + 0.6 * litness;   // 40% dim, 100% bright

        windowGlow = vec3(0.9, 0.75, 0.3)
                * windowBand
                * verticalFace
                * nightFactor
                * occupied
                * 0.35;   // much lower overall intensity
    }

    // --- Combine ---
    vec3 result = baseColor * (ambient + sunDiff * 0.6)
                + baseColor * moonDiffColor * moonDiff
                + vec3(0.9) * sunSpec
                + moonSpecColor * moonSpec
                + windowGlow;

    // --- Fog ---
    float dist = length(cameraPos - fragPos);
    float fogT = clamp((dist - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    fogT       = fogT * fogT;
    result     = mix(result, skyColor, fogT);

    fragColor = vec4(result, 1.0);
}