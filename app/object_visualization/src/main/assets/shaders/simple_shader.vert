attribute vec3 inPosition;//vstup - pozice vrcholu
attribute vec3 inColor;// vstup - barva vrcholu
varying vec3 vertColor;// výstup - barva vrcholu
uniform mat4 vpMatrix;// proměnná - stejná pro všechny vrcholy
uniform vec3 translation;
void main() {
    // zmenšení krychle, posun o translationMatici
    vec3 temp =  vec3(0.4*(inPosition.x + translation.x), 0.4*(inPosition.y +translation.y), 0.4*(inPosition.z + translation.z));
    // násobení view a projection maticí
    gl_Position = vpMatrix * vec4(temp, 1.0);
    // předání barvy fragment shaderu
    vertColor = inColor;
}
