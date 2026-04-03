import os
from pymongo import MongoClient
from bson import ObjectId

USERS_MONGO_URI = os.getenv("USERS_MONGO_URI", "mongodb://users-db:27017/usersdb")
FARMS_MONGO_URI = os.getenv("FARMS_MONGO_URI", "mongodb://farms-db:27017/farmsdb")

users_client = MongoClient(USERS_MONGO_URI)
farms_client = MongoClient(FARMS_MONGO_URI)
def get_mongo_client(uri):
    return MongoClient(uri)

def get_all_users_with_farms():
    users_client = get_mongo_client(USERS_MONGO_URI)
    farms_client = get_mongo_client(FARMS_MONGO_URI)

    try:
        users_db = users_client.get_default_database()
        farms_db = farms_client.get_default_database()

        users_collection = users_db["users"]
        farms_collection = farms_db["farms"]

        all_users = []

        mongo_users = list(users_collection.find())

        for user in mongo_users:
            user_id_str = str(user["_id"])

            user_farms_cursor = farms_collection.find({"userId": user_id_str})

            transformed_farms = []
            for farm in user_farms_cursor:
                crops = []
                active_fields = []

                if "fields" in farm:
                    for field in farm["fields"]:
                        status = field.get("status", "").upper()
                        if status == "PLANTED" and "seedType" in field:
                            raw_seed = field["seedType"]

                            crops.append(raw_seed)
                            field_detail = {
                                "field_id": str(field.get("_id", "unknown")),
                                "seed_type": raw_seed,
                                "growth_stage": field.get("growthStage", "0")
                            }
                            active_fields.append(field_detail)

                transformed_farm = {
                    "id": str(farm["_id"]),
                    "name": farm.get("name", "Unknown Farm"),
                    "latitude": farm.get("latitude"),
                    "longitude": farm.get("longitude"),
                    "soilType": farm.get("soilType"),
                    "crops": list(set(crops)),
                    "fields": active_fields
                }
                transformed_farms.append(transformed_farm)

            transformed_user = {
                "id": user_id_str,
                "name": f"{user.get('firstName', '')} {user.get('lastName', '')}".strip(),
                "email": user.get("email"),
                "farms": transformed_farms
            }

            all_users.append(transformed_user)

        return all_users

    except Exception as e:
        print(f"Error fetching data from MongoDB: {e}")
        return []