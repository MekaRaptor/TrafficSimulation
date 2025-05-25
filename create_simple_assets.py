#!/usr/bin/env python3
"""
Simple Asset Creator for Traffic Simulation
Creates basic PNG assets for vehicles, roads, and traffic lights
"""

from PIL import Image, ImageDraw
import os

def create_directories():
    """Create asset directories"""
    dirs = ['assets/vehicles', 'assets/roads', 'assets/lights', 'assets/environment']
    for dir_path in dirs:
        os.makedirs(dir_path, exist_ok=True)
    print("Asset directories created!")

def create_vehicle_assets():
    """Create simple vehicle sprites"""
    
    # Car (32x32)
    car = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(car)
    # Car body (blue)
    draw.rectangle([4, 10, 28, 22], fill=(70, 130, 180, 255))
    # Windows (light blue)
    draw.rectangle([8, 12, 24, 20], fill=(173, 216, 230, 255))
    # Wheels (black)
    draw.ellipse([4, 8, 10, 14], fill=(50, 50, 50, 255))
    draw.ellipse([4, 18, 10, 24], fill=(50, 50, 50, 255))
    draw.ellipse([22, 8, 28, 14], fill=(50, 50, 50, 255))
    draw.ellipse([22, 18, 28, 24], fill=(50, 50, 50, 255))
    car.save('assets/vehicles/car.png')
    
    # Truck (40x32)
    truck = Image.new('RGBA', (40, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(truck)
    # Truck body (red)
    draw.rectangle([4, 8, 36, 24], fill=(220, 20, 60, 255))
    # Cab (darker red)
    draw.rectangle([28, 10, 36, 22], fill=(178, 34, 34, 255))
    # Windows (light blue)
    draw.rectangle([30, 12, 34, 20], fill=(173, 216, 230, 255))
    # Wheels (black)
    draw.ellipse([6, 6, 12, 12], fill=(50, 50, 50, 255))
    draw.ellipse([6, 20, 12, 26], fill=(50, 50, 50, 255))
    draw.ellipse([18, 6, 24, 12], fill=(50, 50, 50, 255))
    draw.ellipse([18, 20, 24, 26], fill=(50, 50, 50, 255))
    draw.ellipse([30, 6, 36, 12], fill=(50, 50, 50, 255))
    draw.ellipse([30, 20, 36, 26], fill=(50, 50, 50, 255))
    truck.save('assets/vehicles/truck.png')
    
    # Motorcycle (24x32)
    motorcycle = Image.new('RGBA', (24, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(motorcycle)
    # Body (black)
    draw.rectangle([8, 12, 16, 20], fill=(50, 50, 50, 255))
    # Rider (brown)
    draw.ellipse([10, 8, 14, 12], fill=(139, 69, 19, 255))
    # Wheels (black)
    draw.ellipse([6, 6, 12, 12], fill=(50, 50, 50, 255))
    draw.ellipse([6, 20, 12, 26], fill=(50, 50, 50, 255))
    motorcycle.save('assets/vehicles/motorcycle.png')
    
    # Bus (48x32)
    bus = Image.new('RGBA', (48, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bus)
    # Bus body (yellow)
    draw.rectangle([4, 6, 44, 26], fill=(255, 215, 0, 255))
    # Windows (light blue)
    for i in range(6):
        x = 8 + i * 6
        draw.rectangle([x, 10, x+4, 18], fill=(173, 216, 230, 255))
    # Door (darker yellow)
    draw.rectangle([40, 10, 44, 22], fill=(218, 165, 32, 255))
    # Wheels (black)
    draw.ellipse([8, 4, 14, 10], fill=(50, 50, 50, 255))
    draw.ellipse([8, 22, 14, 28], fill=(50, 50, 50, 255))
    draw.ellipse([34, 4, 40, 10], fill=(50, 50, 50, 255))
    draw.ellipse([34, 22, 40, 28], fill=(50, 50, 50, 255))
    bus.save('assets/vehicles/bus.png')
    
    print("Vehicle assets created!")

def create_road_assets():
    """Create road textures"""
    
    # Horizontal road (128x64)
    h_road = Image.new('RGBA', (128, 64), (70, 70, 70, 255))
    draw = ImageDraw.Draw(h_road)
    # Road markings (white dashed line)
    for i in range(0, 128, 16):
        draw.rectangle([i, 30, i+8, 34], fill=(255, 255, 255, 255))
    h_road.save('assets/roads/road_horizontal.png')
    
    # Vertical road (64x128)
    v_road = Image.new('RGBA', (64, 128), (70, 70, 70, 255))
    draw = ImageDraw.Draw(v_road)
    # Road markings (white dashed line)
    for i in range(0, 128, 16):
        draw.rectangle([30, i, 34, i+8], fill=(255, 255, 255, 255))
    v_road.save('assets/roads/road_vertical.png')
    
    # Intersection (64x64)
    intersection = Image.new('RGBA', (64, 64), (60, 60, 60, 255))
    draw = ImageDraw.Draw(intersection)
    # Crosswalk stripes
    for i in range(0, 64, 8):
        draw.rectangle([i, 28, i+4, 36], fill=(255, 255, 255, 255))
        draw.rectangle([28, i, 36, i+4], fill=(255, 255, 255, 255))
    intersection.save('assets/roads/intersection.png')
    
    # Zebra crossing (64x32)
    zebra = Image.new('RGBA', (64, 32), (255, 255, 255, 255))
    draw = ImageDraw.Draw(zebra)
    # Black stripes
    for i in range(0, 64, 8):
        draw.rectangle([i, 0, i+4, 32], fill=(50, 50, 50, 255))
    zebra.save('assets/roads/zebra_crossing.png')
    
    print("Road assets created!")

def create_traffic_light_assets():
    """Create traffic light sprites"""
    
    # Traffic light base (24x64)
    for state, color in [('red', (255, 0, 0)), ('yellow', (255, 255, 0)), ('green', (0, 255, 0))]:
        light = Image.new('RGBA', (24, 64), (0, 0, 0, 0))
        draw = ImageDraw.Draw(light)
        
        # Pole (dark gray)
        draw.rectangle([10, 32, 14, 64], fill=(80, 80, 80, 255))
        
        # Housing (black)
        draw.rectangle([4, 4, 20, 36], fill=(40, 40, 40, 255))
        
        # Light circles (dark when off)
        draw.ellipse([6, 6, 18, 18], fill=(100, 0, 0) if state != 'red' else color)
        draw.ellipse([6, 14, 18, 26], fill=(100, 100, 0) if state != 'yellow' else color)
        draw.ellipse([6, 22, 18, 34], fill=(0, 100, 0) if state != 'green' else color)
        
        # Active light glow
        if state == 'red':
            draw.ellipse([6, 6, 18, 18], fill=color)
        elif state == 'yellow':
            draw.ellipse([6, 14, 18, 26], fill=color)
        elif state == 'green':
            draw.ellipse([6, 22, 18, 34], fill=color)
        
        light.save(f'assets/lights/traffic_light_{state}.png')
    
    print("Traffic light assets created!")

def create_environment_assets():
    """Create environment elements"""
    
    # Building (64x96)
    building = Image.new('RGBA', (64, 96), (139, 69, 19, 255))
    draw = ImageDraw.Draw(building)
    # Windows (yellow)
    for row in range(3):
        for col in range(3):
            x = 8 + col * 16
            y = 8 + row * 24
            draw.rectangle([x, y, x+8, y+12], fill=(255, 255, 200, 255))
    # Door (brown)
    draw.rectangle([24, 72, 40, 96], fill=(101, 67, 33, 255))
    building.save('assets/environment/building.png')
    
    # Tree (32x48)
    tree = Image.new('RGBA', (32, 48), (0, 0, 0, 0))
    draw = ImageDraw.Draw(tree)
    # Trunk (brown)
    draw.rectangle([12, 32, 20, 48], fill=(101, 67, 33, 255))
    # Leaves (green)
    draw.ellipse([4, 4, 28, 36], fill=(34, 139, 34, 255))
    tree.save('assets/environment/tree.png')
    
    # Grass texture (64x64)
    grass = Image.new('RGBA', (64, 64), (34, 139, 34, 255))
    draw = ImageDraw.Draw(grass)
    # Add some texture variation
    for i in range(0, 64, 4):
        for j in range(0, 64, 4):
            variation = (i + j) % 20 - 10
            grass_color = (max(0, 34 + variation), min(255, 139 + variation), max(0, 34 + variation), 255)
            draw.rectangle([i, j, i+4, j+4], fill=grass_color)
    grass.save('assets/environment/grass.png')
    
    print("Environment assets created!")

def main():
    print("Creating simple assets for Traffic Simulation...")
    print("=" * 50)
    
    try:
        create_directories()
        create_vehicle_assets()
        create_road_assets()
        create_traffic_light_assets()
        create_environment_assets()
        
        print("=" * 50)
        print("✅ All assets created successfully!")
        print("\nAsset files created:")
        print("🚗 Vehicles: car.png, truck.png, motorcycle.png, bus.png")
        print("🛣️  Roads: road_horizontal.png, road_vertical.png, intersection.png, zebra_crossing.png")
        print("🚦 Traffic Lights: traffic_light_red.png, traffic_light_yellow.png, traffic_light_green.png")
        print("🏢 Environment: building.png, tree.png, grass.png")
        print("\nNow run your simulation to see the assets in action!")
        
    except ImportError:
        print("❌ PIL (Pillow) library not found!")
        print("Please install it with: pip install Pillow")
        print("\nAlternatively, you can download ready-made assets from:")
        print("- https://kenney.nl/assets/city-kit-roads")
        print("- https://opengameart.org/art-search-advanced?keys=car+sprite")
        print("- https://www.flaticon.com/search?word=traffic")
        
    except Exception as e:
        print(f"❌ Error creating assets: {e}")

if __name__ == "__main__":
    main() 