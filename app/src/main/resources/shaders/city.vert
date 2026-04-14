#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 texCoord;

uniform mat4  model;
uniform mat4  view;
uniform mat4  projection;
uniform float waterLevel;
uniform float tsunamiOriginX;
uniform float tsunamiOriginZ;
uniform float tsunamiTime;
uniform int   tsunamiActive;

out vec3  fragNormal;
out vec3  fragPos;
out vec2  fragTexCoord;
out float floodHeight;

void main() {
    vec4 worldPos = model * vec4(position, 1.0);
    fragPos       = worldPos.xyz;
    fragNormal    = mat3(transpose(inverse(model))) * normal;
    fragTexCoord  = texCoord;

    floodHeight = 0.0;

    if (tsunamiActive == 1) {
        // Distance from THIS vertex to tsunami origin
        float dx   = worldPos.x - tsunamiOriginX;
        float dz   = worldPos.z - tsunamiOriginZ;
        float dist = sqrt(dx*dx + dz*dz);

        // Wave front travels at 40 units/sec — matches Java & ocean shader
        float waveFront     = tsunamiTime * 40.0;
        float arrivalFactor = clamp((waveFront - dist) / 30.0, 0.0, 1.0);

        float floodDecay  = max(0.0, tsunamiTime - 15.0) * 0.3;
        float localFlood  = waterLevel
                          * arrivalFactor
                          * max(0.3, 1.0 - floodDecay * 0.05);

        // How submerged is this vertex
        floodHeight = clamp((localFlood - position.y) / 10.0, 0.0, 1.0);
    }

    gl_Position = projection * view * worldPos;
}