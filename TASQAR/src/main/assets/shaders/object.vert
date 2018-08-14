/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


attribute vec4 a_Position;
attribute vec4 a_Normal;

uniform mat4 u_ModelViewProjection;

uniform float tileCount;

varying vec3  normal;
varying vec2  tileCoord;
varying vec2  texCoord;
varying float ambientOcclusion;

void main() {
    //Compute ambient occlusion
    ambientOcclusion = 0.5;

    //Compute normal
    normal = a_Normal.xyz;

    //Compute texture coordinate
    texCoord = vec2(dot(vec3(a_Position), vec3(normal.y-normal.z, 0, normal.x)),
                      dot(vec3(a_Position), vec3(0, -abs(normal.x+normal.z), normal.y)));

    //Compute tile coordinate
    float tx    = a_Normal.x / tileCount;
    tileCoord.x = floor(tx);
    tileCoord.y = fract(tx) * tileCount;

    gl_Position = u_ModelViewProjection * a_Position;
}
