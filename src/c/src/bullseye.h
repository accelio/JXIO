/*
 ** Copyright (C) 2013 Mellanox Technologies
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at:
 **
 ** http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 ** either express or implied. See the License for the specific language
 ** governing permissions and  limitations under the License.
 **
 */

/*
 * Bullseye Coverage Definitions
*/
#ifndef BULLSEYE_H_
#define BULLSEYE_H_

#if _BullseyeCoverage
#define BULLSEYE_EXCLUDE_BLOCK_START	"BullseyeCoverage save off";
#define BULLSEYE_EXCLUDE_BLOCK_END	"BullseyeCoverage restore";
#else
#define BULLSEYE_EXCLUDE_BLOCK_START
#define BULLSEYE_EXCLUDE_BLOCK_END
#endif


#endif /* BULLSEYE_H_ */
