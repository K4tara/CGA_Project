#version 330 core

layout(location = 0) in vec3 position;
layout(location =  1) in vec2 tc;
layout(location = 2) in vec3 normale;

//uniforms
//translation object to world
uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 projection_matrix;

uniform vec2 tcMultiplier;
uniform vec3 cyclePointLightPos;
uniform vec3 cyclePoint2LightPos;
uniform vec3 cyclePoint3LightPos;
uniform vec3 cycleSpotLightPos;

uniform int chaos;
uniform int confuse;
uniform int shake;
uniform float time;

out struct VertexData{
    vec3 position;
    vec2 texture;
    vec3 normale;
    vec3 toPointLight;
    vec3 toPointLight2;
    vec3 toPointLight3;
    vec3 toSpotLight;
} vertexData;


void main() {
    mat4 modelView = view_matrix * model_matrix;
    vec4 pos =  modelView * vec4(position, 1.0f);
    vec4 nor = inverse(transpose(modelView)) * vec4(normale, 0.0f);

    vec4 lp = view_matrix * vec4(cyclePointLightPos, 1.0); //Position der Lichtquelle im Viewspace
    vertexData.toPointLight = (lp - pos).xyz; //Richtungsvektor der Lichtquelle (im Camera space)

    vec4 lp3 = view_matrix * vec4(cyclePoint2LightPos, 1.0);
    vertexData.toPointLight2 = (lp3 - pos).xyz;

    vec4 lp4 = view_matrix * vec4(cyclePoint3LightPos, 1.0);
    vertexData.toPointLight3 = (lp4 - pos).xyz;

    vec4 lp2 = view_matrix * vec4(cycleSpotLightPos, 1.0);
    vertexData.toSpotLight = (lp2 - pos).xyz;

    gl_Position = projection_matrix * pos;
    vertexData.position = -pos.xyz;
    vertexData.texture = tc * tcMultiplier;
    vertexData.normale = nor.xyz;


    //different effects based on boolean values (int 1 == true)
    if (chaos == 1) {
        float strength = 0.3;
        vec2 pos = vec2(tc.x + sin(time) * strength, tc.y + cos(time) * strength);
        vertexData.texture = pos * tcMultiplier;
    } else {
        vertexData.texture = tc * tcMultiplier;
    }

    if (shake == 1) {
        float strength = 0.5;
        gl_Position.x += cos(time * 10) * strength;
        gl_Position.y += cos(time * 15) * strength;
        //gl_Position.z += cos(time * 10) * strength;
    }
}
