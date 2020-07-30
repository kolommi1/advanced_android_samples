attribute vec3 inPosition;//vstup - pozice vrcholu
attribute vec2 inTexCoord;
varying vec2 varTextCoord;
varying vec3 vertColor;// výstup - barva vrcholu
uniform mat4 vpMatrix;// proměnná - stejná pro všechny vrcholy
uniform vec3 translation;
uniform vec3 rotation;

mat3 rotateY (float w){
    mat3 rotationMat;
    rotationMat[0] = vec3( cos(w), 0, -sin(w));
    rotationMat[1] = vec3( 0,      1, 0);
    rotationMat[2] = vec3( sin(w), 0, cos(w));
    return rotationMat;
}

void main() {
    // zmenšení krychle, posun o translationMatici
    vec3 temp = rotateY(rotation.y)* vec3(4.0*(inPosition.x + translation.x), 4.0*(inPosition.y +translation.y), 4.0*(inPosition.z + translation.z));
    // násobení view a projection maticí
    gl_Position = vpMatrix * vec4(temp, 1.0);
    varTextCoord = inTexCoord;
}
