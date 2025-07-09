package creep.enums

import creep.enums.CreepType.*
import screeps.api.BodyPartConstant

enum class Role(vararg val availableTypes: CreepType) {
    UNASSIGNED,

    // Mining stage 1
    HARVESTER(HarvesterSenior, HarvesterMiddle, HarvesterJunior, WorkerFastX2, WorkerSimple),

    // Mining stage > 2
    MINER(MinerLeader, MinerSenior, MinerMiddle, MinerJunior, MinerIntern),
    COURIER(CarrierMiddle, CarrierJunior, CarrierIntern),

    // Mining stage > 3
    CARRIER(CarrierLeader, CarrierSenior, CarrierMiddle, CarrierJunior, CarrierIntern),

    // Infrastructure
    BUILDER(BuilderMiddle, BuilderJunior, WorkerFastX2, WorkerSimple),
    UPGRADER(UpgraderSenior, UpgraderMiddle, UpgraderJunior, WorkerFastX2, WorkerSimple),
    REPAIRER(RepairerMiddle, RepairerJunior, WorkerFastX2, WorkerSimple),

    SCAVENGER(Scavenger),
    GRAVEDIGGER(Scavenger),

    // Defence
    KNIGHT(KnightSenior, KnightMiddle, KnightJunior),
    RANGER(RangerSenior, RangerMiddle, RangerJunior),
    HEALER(HealerJunior),

    ;

    companion object {
        fun getMostValuableBody(role: Enum<Role>): Array<BodyPartConstant> {
            return when (role) {
                UNASSIGNED -> UNASSIGNED
                HARVESTER -> HARVESTER
                MINER -> MINER
                COURIER -> COURIER
                CARRIER -> CARRIER
                BUILDER -> BUILDER
                UPGRADER -> UPGRADER
                REPAIRER -> REPAIRER
                KNIGHT -> KNIGHT
                RANGER -> RANGER
                HEALER -> HEALER
                else -> return emptyArray()
            }.availableTypes.first().body
        }
    }
}