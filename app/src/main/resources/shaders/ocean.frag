#version 330 core

in vec3 fragNormal;
in vec3 fragPos;

uniform vec3  sunDirection;
uniform vec3  cameraPos;
uniform vec3  skyColor;
uniform float fogStart;
uniform float fogEnd;
uniform float timeOfDay;    // ADD — 0.0 to 1.0
uniform vec3  moonDirection; // ADD — for night specular

out vec4 fragColor;

void main() {
    vec3 norm    = normalize(fragNormal);
    vec3 sunDir  = normalize(sunDirection);
    vec3 viewDir = normalize(cameraPos - fragPos);

    // --- Time of day factors ---
    // How high is the sun — negative means below horizon (night)
    float sunHeight = sunDirection.y;
    // 0.0 = night, 1.0 = full day
    float dayFactor = clamp(sunHeight * 2.0 + 0.2, 0.0, 1.0);
    // Sunset factor — peaks when sun is near horizon
    float sunsetFactor = clamp(1.0 - abs(sunHeight) * 3.0, 0.0, 1.0);

    // --- Water base colors by time of day ---
    // Deep water color shifts with lighting
    vec3 deepDay     = vec3(0.0,  0.15, 0.40);
    vec3 deepSunset  = vec3(0.15, 0.08, 0.12);
    vec3 deepNight = vec3(0.02, 0.04, 0.10);   // dark navy, not brown-black

    vec3 surfDay     = vec3(0.0,  0.45, 0.75);
    vec3 surfSunset  = vec3(0.55, 0.25, 0.10);
    vec3 surfNight = vec3(0.05, 0.08, 0.18);   // slightly lighter blue

    // Blend between night, sunset, and day
    vec3 deepColor = mix(deepNight, deepSunset, sunsetFactor);
    deepColor      = mix(deepColor, deepDay,    dayFactor);

    vec3 surfColor = mix(surfNight, surfSunset, sunsetFactor);
    surfColor      = mix(surfColor, surfDay,    dayFactor);

    float depthT    = clamp(fragPos.y * 0.3 + 0.5, 0.0, 1.0);
    vec3  waterBase = mix(deepColor, surfColor, depthT);

    // --- Sun lighting ---
    float diff    = max(dot(norm, sunDir), 0.0) * dayFactor;
    float ambient = mix(0.05, 0.20, dayFactor); // darker ambient at night

     // --- Sun specular — warm gold at sunset, white at noon ---
    vec3  sunSpecColor = mix(vec3(1.0, 0.6, 0.2), vec3(1.0, 1.0, 0.95),
                             dayFactor);
    vec3  halfDir      = normalize(sunDir + viewDir);
    float rawSpec      = pow(max(dot(norm, halfDir), 0.0), 64.0);

    // Reduce specular blowout when sun is near horizon
    // glareReduce goes to 0.15 at the horizon so it never saturates
    float glareReduce  = clamp(abs(sunHeight) * 3.0 + 0.15, 0.15, 1.0);
    float sunSpec      = min(rawSpec, 0.85) * dayFactor * glareReduce;

    // --- Moon specular at night ---
    // When sun is below horizon, moon provides cold blue-white shimmer
    float nightFactor  = clamp(1.0 - dayFactor * 2.0, 0.0, 1.0);
    vec3  moonDir      = normalize(moonDirection);
    vec3  moonHalf     = normalize(moonDir + viewDir);
    float moonSpec     = pow(max(dot(norm, moonHalf), 0.0), 128.0)
                         * nightFactor * 0.4;
    vec3  moonSpecColor = vec3(0.7, 0.8, 1.0); // cold blue-white

    // --- Sunset horizon glow on water ---
    // When sun is near horizon, facing the sun direction gets
    // an orange reflection streak across the water
    float sunReflect = 0.0;
    if (sunsetFactor > 0.0) {
        // How much is this fragment facing toward the sun
        float facingSun = max(dot(normalize(vec2(norm.x, norm.z)),
                                  normalize(vec2(sunDir.x, sunDir.z))),
                              0.0);
        sunReflect = pow(facingSun, 8.0) * sunsetFactor * sunsetFactor * 0.6;
    }
    vec3 sunsetGlow = vec3(1.0, 0.45, 0.05) * sunReflect;

    // --- Combine ---
    vec3 color = waterBase * (ambient + diff * 0.7)
               + sunSpecColor  * sunSpec  * 0.9
               + moonSpecColor * moonSpec
               + sunsetGlow;
    
     // Hard clamp — prevents any combination of terms blowing out to white
    color = clamp(color, 0.0, 1.0);

    // --- Fog ---
    float dist = length(cameraPos - fragPos);
    float fogT = clamp((dist - fogStart) / (fogEnd - fogStart), 0.0, 1.0);
    fogT       = fogT * fogT;
    color      = mix(color, skyColor, fogT);

    fragColor = vec4(color, 1.0);
}