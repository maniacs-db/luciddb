/*
// Licensed to DynamoBI Corporation (DynamoBI) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  DynamoBI licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at

//   http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
*/

#ifndef Fennel_CalcInit_Included
#define Fennel_CalcInit_Included

FENNEL_BEGIN_NAMESPACE

class FENNEL_CALCULATOR_EXPORT CalcInit
{
public:
    //! CalcInit: Call once at start of world to set up instruction factories
    //! and other boot time setup.
    //!
    //! Warning: Not thread safe
    static CalcInit* instance();
private:
    static CalcInit* _instance;
};


FENNEL_END_NAMESPACE

#endif
// End CalcInit.h
