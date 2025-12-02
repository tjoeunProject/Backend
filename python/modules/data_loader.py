import json

def load_json(file_path):
    """JSON 파일을 로드하여 리스트로 반환"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"파일을 찾을 수 없습니다: {file_path}")
        return []
    except json.JSONDecodeError:
        print(f"JSON 형식이 올바르지 않습니다: {file_path}")
        return []

def save_json(data, file_path=None):
    """데이터를 JSON 문자열로 반환하거나 파일로 저장"""
    json_str = json.dumps(data, ensure_ascii=False, indent=2)
    if file_path:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(json_str)
    return json_str