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

precision highp float;

//uniform sampler2D u_Texture;
//uniform float tileSize;
//uniform float tileCount;

varying vec3  normal;
//varying vec2  tileCoord;
//varying vec2  texCoord;
varying float ambientOcclusion;

void main() {
//      vec2 uv  = texCoord;

//      float weight = 0.0;

//      vec2 tileOffset = 2.0 * tileSize * tileCoord;
//      float denom     = 2.0 * tileSize * tileCount;

      vec4 color = vec4(0.2, 0.9, 0.3, 1.0);

//      for(int dx=0; dx<2; ++dx) {
//         for(int dy=0; dy<2; ++dy) {
//           vec2 offset = 2.0 * fract(0.5 * (uv + vec2(dx, dy)));
//           float w = pow(1.0 - max(abs(offset.x-1.0), abs(offset.y-1.0)), 3.0);
//
//           vec2 tc = (tileOffset + tileSize * offset) / denom;
//           color  += w * texture2D(u_Texture, tc);
//           weight += w;
//         }
//       }

//        color /= weight;

        float light = ambientOcclusion + max(0.15 * dot(normal, vec3(1,1,1)), 0.0);

        gl_FragColor = vec4(color.xyz * light, 1.0);
}
