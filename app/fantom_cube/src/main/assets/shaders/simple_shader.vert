attribute vec3 inPosition;//vstup - pozice vrcholu
attribute vec3 inColor;// vstup - barva vrcholu
varying vec3 vertColor;// výstup - barva vrcholu
uniform mat4 vpMatrix;// proměnná - stejná pro všechny vrcholy
uniform vec3 translation;
uniform vec3 rotation;

mat3 rotateX (float w){
    mat3 rotationMat;
    rotationMat[0] = vec3(1,     0, 0);
    rotationMat[1] = vec3(0,    cos(w), sin(w));
    rotationMat[2] = vec3( 0,	  -sin(w), cos(w));
    return rotationMat;
}

mat3 rotateY (float w){
    mat3 rotationMat;
    rotationMat[0] = vec3( cos(w), 0, -sin(w));
    rotationMat[1] = vec3( 0,      1, 0);
    rotationMat[2] = vec3( sin(w), 0, cos(w));
    return rotationMat;
}

mat3 rotateZ (float w){
    mat3 rotationMat;
    rotationMat[0] = vec3( cos(w),     sin(w), 0);
    rotationMat[1] = vec3(-sin(w),    cos(w), 0);
    rotationMat[2] = vec3( 0,	        0,      1);
    return rotationMat;
}

void main() {
    // otočení na ose Z a Y
    vec3 temp = rotateZ(rotation.z)* rotateY(rotation.y)*rotateX(rotation.x)* inPosition.xyz;
    // zmenšení krychle, posun o translationMatici
    temp = vec3(0.37*(temp.x + translation.x), 0.37*(temp.y +translation.y), 0.37*(temp.z + translation.z));
    // násobení view a projection maticí
    gl_Position = vpMatrix * vec4(temp, 1.0);
    // předání barvy fragment shaderu
    vertColor = inColor;
}
