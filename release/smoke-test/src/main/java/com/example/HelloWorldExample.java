package com.example;

import jadex.bdi.hellopure.HelloPureAgent;
import jadex.core.impl.ComponentManager;

public class HelloWorldExample
{
    public static void main(String[] args)
    {
        ComponentManager.get().create(new HelloPureAgent()).get();
        ComponentManager.get().waitForLastComponentTerminated();
    }
}
