import pytest
from unittest.mock import patch, MagicMock
from database.repository import get_all_users_with_farms

MOCK_MONGO_USERS = [
    {"_id": "user123", "firstName": "John", "lastName": "Doe", "email": "john@test.com"}
]

MOCK_MONGO_FARMS = [
    {
        "_id": "farmABC",
        "userId": "user123",
        "name": "Green Farm",
        "latitude": 45.0,
        "longitude": 20.0,
        "soilType": "CLAY",
        "fields": [
            {"_id": "field1", "status": "PLANTED", "seedType": "CORN", "growthStage": "YOUNG"},
            {"_id": "field2", "status": "EMPTY"}
        ]
    }
]

@patch("database.repository.MongoClient")
def test_get_all_users_with_farms_transformation(MockMongoClient):
    mock_client = MockMongoClient.return_value
    mock_db = mock_client.get_default_database.return_value

    mock_users_collection = MagicMock()
    mock_users_collection.find.return_value = MOCK_MONGO_USERS

    mock_farms_collection = MagicMock()
    mock_farms_collection.find.return_value = MOCK_MONGO_FARMS

    def get_collection_mock(name):
        if name == "users":
            return mock_users_collection
        if name == "farms":
            return mock_farms_collection
        return MagicMock()

    mock_db.__getitem__.side_effect = get_collection_mock
    result = get_all_users_with_farms()

    assert len(result) == 1
    user = result[0]

    assert user["id"] == "user123"
    assert user["email"] == "john@test.com"
    assert len(user["farms"]) == 1

    farm = user["farms"][0]
    assert farm["id"] == "farmABC"

    assert len(farm["fields"]) == 1
    assert farm["fields"][0]["seed_type"] == "CORN"