package com.lilithsthrone.game.inventory.enchanting;

import com.lilithsthrone.game.character.body.Fluid;

public enum EffectTarget {

    SELF("self"),

    CUM("cum"),

    MILK("milk"),

    GIRLCUM("girlcum"),

    CROTCHMILK("crotchmilk");

    String name;

    EffectTarget(String name){
        this.name = name;
    }

    public boolean isCorrectFluid(Fluid fluid){
        switch (fluid.getType().getSource()){

            case PENIS:
                return this == CUM;
            case BREAST:
                return this == MILK;
            case VAGINA:
                return this == GIRLCUM;
            case BREAST_CROTCH:
                return this == CROTCHMILK;
        }
        return false;
    }
}
