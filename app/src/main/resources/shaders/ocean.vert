#version 330 core

layout(location = 0) in vec3 position;

uniform float time;
uniform float amplitude;
uniform float frequency;
uniform float speed;
uniform vec2  direction;
uniform float tidalOffset;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform int   tsunamiActive;
uniform vec2  tsunamiOrigin;
uniform float tsunamiTime;

out vec3 fragNormal;
out vec3 fragPos;

// -----------------------------------------------------------
// Normal ocean Gerstner wave — used only when no tsunami
// -----------------------------------------------------------
vec3 gerstnerDisplace(vec3 pos, vec2 dir, float amp,
                      float freq, float spd, float t) {
    float phase = dot(dir, pos.xz) * freq - t * spd;
    return vec3(
        amp * cos(phase) * dir.x,
        amp * sin(phase),
        amp * cos(phase) * dir.y
    );
}

vec3 gerstnerNormal(vec3 pos, vec2 dir, float amp,
                    float freq, float spd, float t) {
    float phase = dot(dir, pos.xz) * freq - t * spd;
    float cosP  = cos(phase);
    return vec3(
        -dir.x * freq * amp * cosP,
         0.0,
        -dir.y * freq * amp * cosP
    );
}

void main() {
    vec3 pos = position;

    // Shore mask — waves flatten as they reach land
    float shoreMask        = smoothstep(15.0, -60.0,  pos.z);
    float tsunamiShoreMask = smoothstep(300.0, 0.0, pos.z);    
    float effectiveMask = tsunamiActive == 1
        ? tsunamiShoreMask
        : shoreMask;

    vec3 n = vec3(0.0, 1.0, 0.0);

    if (tsunamiActive == 0) {
        // ---------------------------------------------------
        // NORMAL OCEAN — three Gerstner waves toward shore
        // All directions have positive Z to travel toward city
        // ---------------------------------------------------
        float safeAmp = amplitude * effectiveMask;

        vec2 dir0 = normalize(vec2( 0.0,  1.0));
        vec2 dir1 = normalize(vec2(-0.3,  1.0));
        vec2 dir2 = normalize(vec2( 0.25, 1.0));

        pos += gerstnerDisplace(pos, dir0, safeAmp,
                                frequency, speed, time);
        pos += gerstnerDisplace(pos, dir1, safeAmp * 0.6,
                                frequency * 1.3, speed * 0.8, time);
        pos += gerstnerDisplace(pos, dir2, safeAmp * 0.4,
                                frequency * 1.8, speed * 1.1, time);

        n += gerstnerNormal(position, dir0, safeAmp,
                            frequency, speed, time);
        n += gerstnerNormal(position, dir1, safeAmp * 0.6,
                            frequency * 1.3, speed * 0.8, time);
        n += gerstnerNormal(position, dir2, safeAmp * 0.4,
                            frequency * 1.8, speed * 1.1, time);

        pos.y += tidalOffset;

    } else {
        // ---------------------------------------------------
        // TSUNAMI MODE
        //
        // Real tsunami physics:
        //
        // 1. In deep ocean the wave is barely visible —
        //    maybe 1-2m tall but hundreds of km long.
        //    It travels at ~800 km/h in deep water.
        //
        // 2. As depth decreases (approaching shore) the wave
        //    SLOWS DOWN but grows TALLER (shoaling effect).
        //    Energy is conserved so height increases.
        //
        // 3. Before the wave arrives the shore experiences
        //    DRAWBACK — water visibly recedes, exposing the
        //    sea floor. This is the warning sign.
        //
        // 4. The wave arrives as a SINGLE SMOOTH WALL of
        //    water — not choppy, not breaking into peaks.
        //    It looks like the tide coming in 100x faster.
        //
        // 5. The wave DOES NOT BREAK like a surf wave.
        //    It surges inland continuously.
        //
        // We kill all Gerstner chop during tsunami and replace
        // with a single smooth long-period wave.
        // ---------------------------------------------------

        float dist      = length(position.xz - tsunamiOrigin);
        float waveSpeed = 40.0;
        float waveFront = tsunamiTime * waveSpeed;

        // How far is this vertex from the wave front
        // Negative = wave has already passed this point
        float distFromFront = dist - waveFront;

        // --- Shoaling ---
        // Wave gets taller closer to shore
        // Shore is at z=15, deep ocean at z=-300
        // Map z from -300..15 to shoaling factor 1.0..4.0
        float depth        = clamp((-pos.z + 15.0) / 315.0, 0.0, 1.0);
        float shoaling     = 1.0 + (1.0 - depth) * 3.0;
        // Apply shore mask so the wave doesn't go under land
        shoaling *= tsunamiShoreMask;

        // --- Phase 1: Drawback ---
        // Water recedes before the wave arrives
        // Affects vertices within 120 units ahead of wave front
        float drawback = 0.0;
        if (distFromFront > 0.0 && distFromFront < 120.0) {
            float drawT = distFromFront / 120.0;
            // Smooth receding curve — max pullback midway
            drawback = -3.5 * sin(drawT * 3.14159) * shoaling * 0.4;
        }

        // --- Phase 2: The main wave wall ---
        // A single smooth hump — very wide (long wavelength)
        // and tall. NOT choppy. The envelope is asymmetric:
        // gradual rise on the back, steep face on the front.
        float mainWave = 0.0;
        float frontWidth = 25.0;   // steep front face
        float backWidth  = 80.0;   // long gradual back slope

        float envelope;
        if (distFromFront <= 0.0) {
            // Wave has passed — gradual back slope
            envelope = exp(-(distFromFront * distFromFront)
                           / (backWidth * backWidth));
        } else {
            // Wave approaching — steeper front face
            envelope = exp(-(distFromFront * distFromFront)
                           / (frontWidth * frontWidth));
        }

        // Base height 15 units, grows with shoaling near shore
        mainWave = 15.0 * envelope * shoaling;

        // --- Phase 3: Surge ---
        // After the main wave passes, a sustained elevated
        // water level floods inland. This is modeled as a
        // slow-decaying offset that follows behind the wave.
        float surge = 0.0;
        if (distFromFront < -30.0) {
            // Behind the wave front by at least 30 units
            float surgeDecay = clamp(
                (-distFromFront - 30.0) / 200.0, 0.0, 1.0);
            surge = 6.0 * (1.0 - surgeDecay) * shoaling * 0.5;
        }

        // Small residual chop on the back of the wave
        // Real tsunamis do have turbulence behind the wall
        float chop = 0.0;
        if (distFromFront < -20.0 && distFromFront > -150.0) {
            float chopPhase = pos.z * 0.3 - time * 2.0;
            chop = 1.5 * sin(chopPhase) * envelope * 0.3;
        }

        pos.y += mainWave + drawback + surge + chop;

        // Normal — slope based on wave gradient
        vec2 toOrigin = normalize(tsunamiOrigin - position.xz
                                  + vec2(0.001));
        float slope;
        if (distFromFront <= 0.0) {
            slope = 2.0 * (-distFromFront)
                    / (backWidth * backWidth)
                    * 15.0 * shoaling * envelope;
        } else {
            slope = -2.0 * distFromFront
                    / (frontWidth * frontWidth)
                    * 15.0 * shoaling * envelope;
        }
        n.x += toOrigin.x * slope * 0.5;
        n.z += toOrigin.y * slope * 0.5;
    }

    fragNormal  = normalize(n);
    fragPos     = vec3(model * vec4(pos, 1.0));
    gl_Position = projection * view * model * vec4(pos, 1.0);
}