import json

with open('postman/aeg-core.postman_collection.json', 'r') as f:
    data = json.load(f)

# We will collect all delete requests and remove them from their original folders
delete_requests = []

for folder in data['item']:
    if 'item' in folder:
        new_items = []
        for req in folder['item']:
            if req['name'].startswith('Delete '):
                delete_requests.append(req)
            else:
                new_items.append(req)
        folder['item'] = new_items

# Reverse the delete requests to avoid FK constraints
delete_requests.reverse()

# Add a Teardown folder at the end
teardown_folder = {
    "name": "Teardown",
    "item": delete_requests
}

data['item'].append(teardown_folder)

with open('postman/aeg-core.postman_collection.json', 'w') as f:
    json.dump(data, f, indent=2)

print("Reorganized successfully!")
