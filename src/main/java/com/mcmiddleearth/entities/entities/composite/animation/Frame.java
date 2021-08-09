package com.mcmiddleearth.entities.entities.composite.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcmiddleearth.entities.entities.composite.BakedAnimationEntity;
import com.mcmiddleearth.entities.entities.composite.bones.Bone;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Frame {

    private final Map<Bone,BoneData> bones = new HashMap<>();

    public Frame() {
    }

    public void apply(int state) {
        bones.forEach((bone, boneData) -> {
            bone.setRelativePosition(boneData.getPosition());
/*if(bone.getName().equals("bone")) {
    Logger.getGlobal().info("Frame position: "+bone.getRelativePosition().toString());
}*/
            bone.setHeadPose(boneData.getHeadPose());
            bone.setHeadItem(boneData.getItems()[state]);
        });
    }

    public static Frame loadFrame(BakedAnimationEntity entity, BakedAnimation animation,
                                  JsonObject data, Material itemMaterial, int headPoseDelay) {
        Set<Map.Entry<String, JsonElement>> entries = data.get("bones").getAsJsonObject().entrySet();
//long start = System.currentTimeMillis();
        Frame frame = new Frame();
        entries.forEach(entry-> {
            BoneData boneData = BoneData.loadBoneData(entity.getStates(),entry.getValue().getAsJsonObject(),itemMaterial);
            Bone bone = entity.getBones().stream().filter(searchBone->entry.getKey().equals(searchBone.getName())).findFirst().orElse(null);
            if(bone == null) {
//long boneStart = System.currentTimeMillis();
                bone = new Bone(entry.getKey(), entity, boneData.getHeadPose(),
                                boneData.getPosition(), boneData.getItems()[0],entry.getKey().startsWith("head"), headPoseDelay);
//Logger.getGlobal().info("Bone creation: "+(System.currentTimeMillis()-boneStart));
                entity.getBones().add(bone);
                /*if(bone.getName().startsWith("head")) {
                    entity.getHeadBones().add(bone);
                }*/
//Logger.getGlobal().info("create bone at: "+bone.getLocation());
            }
            frame.bones.put(bone,boneData);
        });
//Logger.getGlobal().info("Frame loading: "+(System.currentTimeMillis()-start));
        return frame;
    }
}
