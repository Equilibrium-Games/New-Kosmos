#version 130

//---------CONSTANT------------
const float PI = 3.1415926535897932384626433832795;
const float SHADOW_TRANSITION = 10.0;

//---------IN------------
layout(location = 0) in vec3 in_position;

//---------UNIFORM------------
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform vec4 clipPlane;
uniform mat4 modelMatrix;
uniform mat4 shadowSpaceMatrix;
uniform float shadowDistance;

uniform float waveLength;
uniform float amplitude;

uniform float waveTime;
uniform float waterHeight;
uniform float squareSize;

//---------OUT------------
out vec4 pass_positionRelativeToCam;
out vec3 pass_surfaceNormal;
out vec4 pass_shadowCoords;
out vec4 pass_clipSpace;

//---------WAVE OFFSET------------
float generateOffset(float x, float z, float val1, float val2){
	float radiansX = ((mod(x + z * x * val1, waveLength) / waveLength) + waveTime) * 2.0 * PI;
	float radiansZ = ((mod(val2 * (z * x + x * z), waveLength) / waveLength) + waveTime * 2.0) * 2.0 * PI;
	return amplitude * 0.5 * (sin(radiansZ) + sin(radiansX));
}

//---------VERTEX OFFSET------------
vec3 generateVertexOffset(float x, float z){
	return vec3(generateOffset(x, z, 0.2, 0.1), generateOffset(x, z, 0.1, 0.3), generateOffset(x, z, 0.15, 0.2));
}

//---------DECODE------------
vec4 decode(float n){
	float delta = mod(n, 3.0);
	float z2 = delta - 1.0;
	n =  (n - delta) / 3.0;
	delta = mod(n, 3.0);
	float x2 = delta - 1.0;
	n =  (n - delta) / 3.0;
	delta = mod(n, 3.0);
	float z1 = delta - 1.0;
	n =  (n - delta) / 3.0;
	float x1 = n - 1.0;
	return vec4(x1, z1, x2, z2);
}

//---------MAIN------------
void main(void) {
	vec4 offsets = decode(in_position.z);

	vec3 thisVertex = vec3(in_position.x, waterHeight, in_position.y);
	vec3 otherVertex1 = vec3(in_position.x + (offsets.z * squareSize), waterHeight, in_position.y + (offsets.w * squareSize));
	vec3 otherVertex2 = vec3(in_position.x + (offsets.x * squareSize), waterHeight, in_position.y + (offsets.y * squareSize));

	thisVertex += generateVertexOffset(thisVertex.x, thisVertex.z);
	otherVertex1 += generateVertexOffset(otherVertex1.x, otherVertex1.z);
	otherVertex2 += generateVertexOffset(otherVertex2.x, otherVertex2.z);

    pass_clipSpace = projectionMatrix * viewMatrix * vec4(thisVertex, 1.0);

	vec4 worldPosition = modelMatrix * vec4(thisVertex, 1.0);
	pass_positionRelativeToCam = viewMatrix * worldPosition;

	gl_ClipDistance[0] = dot(worldPosition, clipPlane);
	gl_Position = projectionMatrix * pass_positionRelativeToCam;

	vec3 tangent = otherVertex1 - thisVertex;
    vec3 bitangent = otherVertex2 - thisVertex;
    vec3 normal = -cross(tangent, bitangent);

	pass_surfaceNormal = normalize(normal);

		pass_shadowCoords = shadowSpaceMatrix * worldPosition;
    	float distanceAway = length(pass_positionRelativeToCam.xyz);
        distanceAway = distanceAway - ((shadowDistance * 2.0) - SHADOW_TRANSITION);
        distanceAway = distanceAway / SHADOW_TRANSITION;
        pass_shadowCoords.w = clamp(1.0 - distanceAway, 0.0, 1.0);
}
