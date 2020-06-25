package me.nowaha.dailyquests;

public class Quest
{
    String name;
    String description;
    QuestType questType;
    RewardType rewardType;
    String rewardData;
    String color;
    
    public Quest(final String name, final String description, final QuestType questType, final RewardType rewardType, final String rewardData) {
        this.name = name;
        this.description = description;
        this.questType = questType;
        this.rewardType = rewardType;
        this.rewardData = rewardData;
    }
    
    public enum QuestType
    {
        SELL_ITEMS, 
        COLLECT_ITEMS, 
        SMELT_ITEMS, 
        KILL_MOBS;
    }
    
    public enum RewardType
    {
        POINTS, 
        ITEM, 
        CASH;
    }
}
