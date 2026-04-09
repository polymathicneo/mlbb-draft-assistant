import json
import requests
import os
from concurrent.futures import ThreadPoolExecutor

# Config
DATA_URL = "https://raw.githubusercontent.com/p3hndrx/MLBB-API/main/v1/hero-meta-final.json"
IMAGE_DIR = "dataset/portraits"

def download_image(hero):
    name = hero['name']
    # Portrait URL (based on API schema)
    # The API might provide a 'portrait' or 'image' field
    # Example URL: https://akmweb.youngjoygame.com/hero/portrait/101.png
    url = f"https://akmweb.youngjoygame.com/hero/portrait/{hero['id']}.png"
    
    save_path = os.path.join(IMAGE_DIR, f"{name}.png")
    
    if os.path.exists(save_path):
        print(f"Skipping {name}")
        return

    try:
        response = requests.get(url, stream=True, timeout=10)
        if response.status_code == 200:
            with open(save_path, 'wb') as f:
                f.write(response.content)
            print(f"Downloaded {name}")
        else:
            print(f"Failed to download {name}: {response.status_code}")
    except Exception as e:
        print(f"Error downloading {name}: {e}")

def main():
    if not os.path.exists(IMAGE_DIR):
        os.makedirs(IMAGE_DIR)

    print("Fetching hero metadata...")
    response = requests.get(DATA_URL)
    if response.status_code != 200:
        print("Failed to fetch metadata")
        return

    heroes = response.json()
    print(f"Found {len(heroes)} heroes. Starting download...")

    with ThreadPoolExecutor(max_workers=5) as executor:
        executor.map(download_image, heroes)

if __name__ == "__main__":
    main()
