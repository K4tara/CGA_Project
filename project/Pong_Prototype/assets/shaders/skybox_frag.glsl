#version 330 core
out vec4 FragColor;

in vec3 TexCoords;

uniform samplerCube skybox;

uniform int chaos;
uniform int confuse;
uniform int shake;
uniform float time;

void main() {
    //FragColor = texture(skybox, TexCoords);
    vec4 result = texture(skybox, TexCoords);

    vec2 tex = TexCoords.xy;

    // effects
    if (chaos == 1) {
        result += vec4(abs(sin(cos((time+3*tex.y)*2*tex.x+time))),abs(cos(sin((time+2*tex.x)*3*tex.y+time))), 100, 1.0);
        FragColor = result;
    } else if (confuse == 1) {
        FragColor = vec4(1.0 - result.rgba);
    } else {
        FragColor = result;
    }
}
