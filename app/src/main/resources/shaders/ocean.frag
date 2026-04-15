#version 330 core

in vec3 fragNormal;
in vec3 fragPos;

uniform vec3  sunDirection;
uniform vec3  cameraPos;
uniform vec3  skyColor;
uniform float fogStart;
uniform float fogEnd;
uniform float timeOfDay;
uniform vec3  moonDirection;
uniform float time;           // ADD — needed for animated foam
uniform float amplitude;      // ADD — so foam threshold scales with wave height
uniform int   tsunamiActive;  // ADD — tsunami gets different foam behavior

out vec4 fragColor;

void main() {
    vec3 norm    = normalize(fragNormal);
    vec3 sunDir  = normalize(sunDirection);
    vec3 viewDir = normalize(cameraPos - fragPos);

    // --- Time of day factors ---
    float sunHeight   = sunDirection.y;
    float dayFactor   = clamp(sunHeight * 2.0 + 0.2, 0.0, 1.0);
    float sunsetFactor= clamp(1.0 - abs(sunHeight) * 3.0, 0.0, 1.0);
    float nightFactor = clamp(1.0 - dayFactor * 2.0, 0.0, 1.0);

    // --- Water base colors by time of day ---
    vec3 deepDay    = vec3(0.0,  0.15, 0.40);
    vec3 deepSunset = vec3(0.15, 0.08, 0.12);
    vec3 deepNight  = vec3(0.02, 0.04, 0.10);
    vec3 surfDay    = vec3(0.0,  0.45, 0.75);
    vec3 surfSunset = vec3(0.55, 0.25, 0.10);
    vec3 surfNight  = vec3(0.05, 0.08, 0.18);

    vec3 deepColor = mix(deepNight, deepSunset, sunsetFactor);
    deepColor      = mix(deepColor, deepDay,    dayFactor);
    vec3 surfColor = mix(surfNight, surfSunset, sunsetFactor);
    surfColor      = mix(surfColor, surfDay,    dayFactor);

    float depthT    = clamp(fragPos.y * 0.3 + 0.5, 0.0, 1.0);
    vec3  waterBase = mix(deepColor, surfColor, depthT);

    // -------------------------------------------------------
    // FOAM / WHITECAPS
    //
    // Real whitecaps form when:
    //   1. The wave crest is steep enough to break
    //   2. Wind has been blowing long enough (we fake this)
    //
    // We approximate with two signals:
    //   A. Height threshold — fragment is near the crest
    //      (fragPos.y above some fraction of amplitude)
    //   B. Normal steepness — the surface is tilting steeply
    //      (norm.y is small = nearly vertical = breaking)
    //
    // We also add animated noise using sin/cos of position
    // and time to break up the foam into irregular patches
    // rather than a clean band.
    //
    // Tsunami foam is much more aggressive — the whole front
    // face of the wave churns white.
    // -------------------------------------------------------

    float foamAmount = 0.0;

    // --- Signal A: height-based crest foam ---
    // Foam appears on the top ~30% of wave height
    float waveAmp      = max(amplitude, 0.5);
    float crestThresh  = waveAmp * 0.55;  // below this Y = no foam
    float heightSignal = clamp(
        (fragPos.y - crestThresh) / (waveAmp * 0.45),
        0.0, 1.0);

    // --- Signal B: steepness foam ---
    // norm.y close to 0 = nearly vertical = breaking wave face
    float steepness     = 1.0 - abs(norm.y);
    float steepSignal   = clamp((steepness - 0.3) / 0.5, 0.0, 1.0);

    // --- Animated noise to break up foam into patches ---
    // Use cheap sin/cos noise on world position + time
    float noiseA = sin(fragPos.x * 1.7 + time * 1.3)
                 * cos(fragPos.z * 2.1 - time * 0.9);
    float noiseB = sin(fragPos.x * 3.1 - time * 2.0)
                 * cos(fragPos.z * 1.4 + time * 1.6);
    float noise  = noiseA * 0.5 + noiseB * 0.5;  // -1 to 1
    // Shift noise so most patches appear on crests not troughs
    float foamNoise = clamp(noise * 0.4 + 0.6, 0.0, 1.0);

    // --- Combine signals ---
    // Both height AND steepness contribute, noise breaks up edges
    foamAmount = (heightSignal * 0.7 + steepSignal * 0.3) * foamNoise;

    // --- Tsunami foam ---
    // During tsunami the wave face is much taller and steeper
    // so we lower the threshold and increase the foam coverage
    if (tsunamiActive == 1) {
        float tsunamiCrest  = clamp(
            (fragPos.y - 3.0) / 8.0,   // foam starts at y=3
            0.0, 1.0);
        float tsunamiSteep  = clamp((steepness - 0.2) / 0.4, 0.0, 1.0);
        float tsunamiFoam   = (tsunamiCrest * 0.6 + tsunamiSteep * 0.4)
                              * foamNoise;

        // Blend — take whichever is stronger
        foamAmount = max(foamAmount, tsunamiFoam);
    }

    foamAmount = clamp(foamAmount, 0.0, 1.0);

    // --- Foam color ---
    // Pure white in daylight, slightly blue-grey at night
    vec3 foamDay   = vec3(1.0,  1.0,  1.0);
    vec3 foamNight = vec3(0.75, 0.80, 0.90);
    vec3 foamColor = mix(foamNight, foamDay, dayFactor);

    // --- Sun lighting ---
    float diff    = max(dot(norm, sunDir), 0.0) * dayFactor;
    float ambient = mix(0.05, 0.20, dayFactor);

    vec3  sunSpecColor = mix(vec3(1.0, 0.6, 0.2), vec3(1.0, 1.0, 0.95),
                             dayFactor);
    vec3  halfDir      = normalize(sunDir + viewDir);
    float rawSpec      = pow(max(dot(norm, halfDir), 0.0), 64.0);
    float glareReduce  = clamp(abs(sunHeight) * 3.0 + 0.15, 0.15, 1.0);
    float sunSpec      = min(rawSpec, 0.85) * dayFactor * glareReduce;

    // --- Moon specular ---
    vec3  moonDir      = normalize(moonDirection);
    vec3  moonHalf     = normalize(moonDir + viewDir);
    float moonSpec     = pow(max(dot(norm, moonHalf), 0.0), 128.0)
                         * nightFactor * 0.4;
    vec3  moonSpecColor = vec3(0.7, 0.8, 1.0);

    // --- Sunset glow ---
    float sunReflect = 0.0;
    if (sunsetFactor > 0.0) {
        float facingSun = max(dot(normalize(vec2(norm.x, norm.z)),
                                  normalize(vec2(sunDir.x, sunDir.z))),
                              0.0);
        sunReflect = pow(facingSun, 8.0) * sunsetFactor * sunsetFactor * 0.6;
    }
    vec3 sunsetGlow = vec3(1.0, 0.45, 0.05) * sunReflect;

    // --- Combine water + foam ---
    vec3 waterColor = waterBase * (ambient + diff * 0.7)
                    + sunSpecColor  * sunSpec * 0.9
                    + moonSpecColor * moonSpec
                    + sunsetGlow;

    // Foam is lit by ambient + diffuse but not specular
    // (foam is matte, not shiny)
    vec3 litFoam = foamColor * (ambient + diff * 0.5);

    // Blend foam over water
    vec3 color = mix(waterColor, litFoam, foamAmount);

    color = clamp(color, 0.0, 1.0);

    // --- Fog ---
    float dist = length(cameraPos - fragPos);
    float fogT = clamp((dist - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    fogT       = fogT * fogT;
    color      = mix(color, skyColor, fogT);

    float viewDot    = abs(dot(norm, viewDir));
    float baseAlpha  = 0.82f;                          // base transparency
    float fresnelAlpha = mix(0.55, baseAlpha, viewDot); // edges more transparent
    float finalAlpha = mix(fresnelAlpha, 1.0, foamAmount); // foam stays opaque
    fragColor = vec4(color, finalAlpha);
}