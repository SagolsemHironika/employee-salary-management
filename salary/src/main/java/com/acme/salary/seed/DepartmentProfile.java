package com.acme.salary.seed;

import java.util.Map;

record DepartmentProfile(String name, String bandPrefix, double weight, Map<Integer, String> titlesByLevel) {}
