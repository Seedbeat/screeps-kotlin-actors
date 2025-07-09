package creep.enums

import screeps.api.*
import screeps.utils.lazyPerTick

enum class CreepType(vararg body: BodyPartConstant) {
//    MOVE = 50
//    WORK = 100
//    CARRY = 50
//    ATTACK = 80
//    RANGED_ATTACK = 150
//    TOUGH = 10
//    HEAL = 250
//    CLAIM = 600

    None,
    WorkerSimple(WORK, CARRY, MOVE      ), // 200
    WorkerFastX2(WORK, CARRY, MOVE, MOVE), // 250

    HarvesterJunior(WORK,                   CARRY,        MOVE, MOVE            ), // 250
    HarvesterMiddle(WORK, WORK,             CARRY, CARRY, MOVE, MOVE, MOVE, MOVE), // 500
    HarvesterSenior(WORK, WORK, WORK, WORK, CARRY, CARRY, MOVE, MOVE, MOVE, MOVE), // 700

    BuilderJunior(WORK,             CARRY, CARRY, CARRY, MOVE, MOVE), // 350
    BuilderMiddle(WORK, WORK, WORK, CARRY, CARRY, CARRY, MOVE, MOVE), // 550

    RepairerJunior(WORK, CARRY,        MOVE, MOVE      ), // 250
    RepairerMiddle(WORK, CARRY, CARRY, MOVE, MOVE, MOVE), // 350

    UpgraderJunior(WORK,                   CARRY, CARRY, CARRY,                      MOVE, MOVE            ), // 350
    UpgraderMiddle(WORK, WORK,             CARRY, CARRY, CARRY, CARRY,               MOVE, MOVE, MOVE      ), // 550
    UpgraderSenior(WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, MOVE, MOVE, MOVE, MOVE), // 900

    MinerIntern(WORK,                                     MOVE), // 150
    MinerJunior(WORK, WORK,                               MOVE), // 250
    MinerMiddle(WORK, WORK, WORK,                         MOVE), // 350
    MinerSenior(WORK, WORK, WORK, WORK, WORK,             MOVE), // 550
    MinerLeader(WORK, WORK, WORK, WORK, WORK, WORK, WORK, MOVE), // 750

    CarrierIntern(CARRY,                                    MOVE                              ), // 100
    CarrierJunior(CARRY, CARRY,                             MOVE, MOVE                        ), // 200
    CarrierMiddle(CARRY, CARRY, CARRY,                      MOVE, MOVE, MOVE                  ), // 300
    CarrierSenior(CARRY, CARRY, CARRY, CARRY,               MOVE, MOVE, MOVE, MOVE            ), // 400
    CarrierLeader(CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE), // 600

    Scavenger(CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE), // 700

    KnightJunior(TOUGH, TOUGH,                      ATTACK,                                 MOVE, MOVE, MOVE            ), // 250
    KnightMiddle(TOUGH, TOUGH, TOUGH, TOUGH, TOUGH, ATTACK, ATTACK, ATTACK,                 MOVE, MOVE, MOVE, MOVE      ), // 510
    KnightSenior(TOUGH, TOUGH, TOUGH, TOUGH, TOUGH, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, MOVE, MOVE, MOVE, MOVE, MOVE), // 700

    RangerJunior(RANGED_ATTACK,                MOVE, MOVE                                    ), // 250
    RangerMiddle(RANGED_ATTACK,                MOVE, MOVE, MOVE, MOVE, MOVE                  ), // 400
    RangerSenior(RANGED_ATTACK, RANGED_ATTACK, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE), // 700

    HealerJunior(HEAL, CARRY, MOVE), // 350

    ;

    val body by lazyPerTick { body.unsafeCast<Array<BodyPartConstant>>() }
    val cost by lazyPerTick { body.sumOf { BODYPART_COST[it]!! } }
}