import re
from collections import defaultdict
from pathlib import Path

ROOT = Path('backend/BlueCrab/src/main/java')
OUTPUT = Path('docs/backend-api-catalog.md')

MAPPING_REGEX = re.compile(r'@(Get|Post|Put|Delete|Patch|Request)Mapping')
REQUEST_METHOD_REGEX = re.compile(r'RequestMethod\.([A-Z]+)')
PATH_REGEX = re.compile(r'"([^"]*)"')
CLASS_REGEX = re.compile(r'class\s+(\w+)')


def extract_base_paths(header_text: str) -> list[str]:
    paths: list[str] = []
    for match in re.finditer(r'@RequestMapping\s*\((.*?)\)', header_text, re.S):
        segment = match.group(1)
        segment_paths = PATH_REGEX.findall(segment)
        if segment_paths:
            paths.extend(segment_paths)
    if not paths:
        paths.append('')
    return sorted(set(paths))


def normalize_path(path: str) -> str:
    if not path:
        return '/'
    result = path
    if not result.startswith('/'):
        result = '/' + result
    while '//' in result:
        result = result.replace('//', '/')
    if len(result) > 1 and result.endswith('/'):
        result = result[:-1]
    return result


def join_paths(base: str, fragment: str) -> str:
    base = base or ''
    fragment = fragment or ''
    if not base and not fragment:
        return '/'
    if not base:
        return normalize_path(fragment)
    if not fragment:
        return normalize_path(base)
    joined = base.rstrip('/') + '/' + fragment.lstrip('/')
    return normalize_path(joined)


def find_class_info(lines: list[str]) -> tuple[str, int]:
    for idx, line in enumerate(lines):
        match = CLASS_REGEX.search(line)
        if match:
            return match.group(1), idx
    raise ValueError('Class not found')


def collect_annotation(lines: list[str], start_index: int) -> tuple[str, int]:
    collected = []
    open_parens = 0
    index = start_index
    while index < len(lines):
        text = lines[index].strip()
        collected.append(text)
        open_parens += text.count('(') - text.count(')')
        if open_parens <= 0:
            break
        index += 1
    annotation = ' '.join(collected)
    return annotation, index


def detect_http_methods(annotation: str, mapping_type: str) -> list[str]:
    if mapping_type == 'GetMapping':
        return ['GET']
    if mapping_type == 'PostMapping':
        return ['POST']
    if mapping_type == 'PutMapping':
        return ['PUT']
    if mapping_type == 'DeleteMapping':
        return ['DELETE']
    if mapping_type == 'PatchMapping':
        return ['PATCH']
    methods = REQUEST_METHOD_REGEX.findall(annotation)
    return methods or ['REQUEST']


def extract_method_name(lines: list[str], start_index: int) -> str:
    index = start_index + 1
    while index < len(lines):
        stripped = lines[index].strip()
        if not stripped:
            index += 1
            continue
        if stripped.startswith('@') and 'Mapping' not in stripped:
            index += 1
            continue
        if stripped.startswith('@') and 'Mapping' in stripped:
            # another mapping annotation; stop
            break
        if ' class ' in stripped:
            break
        if stripped.startswith('public '):
            match = re.search(r'([A-Za-z0-9_]+)\s*\(', stripped)
            if match:
                return match.group(1)
            break
        index += 1
    return ''


def main() -> None:
    controller_entries: dict[str, dict[str, list[tuple[str, str, str]]]] = {}

    for controller_file in ROOT.rglob('*Controller.java'):
        with controller_file.open(encoding='utf-8') as handle:
            lines = [line.rstrip('\n') for line in handle]

        try:
            class_name, class_line_index = find_class_info(lines)
        except ValueError:
            continue

        header_text = '\n'.join(lines[:class_line_index])
        base_paths = extract_base_paths(header_text)

        entries: list[tuple[str, str, str]] = []
        line_index = 0
        while line_index < len(lines):
            stripped = lines[line_index].strip()
            if not stripped.startswith('@') or 'Mapping' not in stripped:
                line_index += 1
                continue

            mapping_match = MAPPING_REGEX.search(stripped)
            if not mapping_match:
                line_index += 1
                continue

            mapping_type = mapping_match.group(1) + 'Mapping'
            annotation, end_index = collect_annotation(lines, line_index)

            # Skip class-level RequestMapping annotations
            if mapping_type == 'RequestMapping' and line_index < class_line_index:
                line_index = end_index + 1
                continue

            http_methods = detect_http_methods(annotation, mapping_type)
            method_paths = PATH_REGEX.findall(annotation) or ['']
            method_name = extract_method_name(lines, end_index)

            for base_path in base_paths:
                for method_path in method_paths:
                    full_path = join_paths(base_path, method_path)
                    for http_method in http_methods:
                        entries.append((http_method, full_path, method_name))

            line_index = end_index + 1

        deduped = sorted(set(entries), key=lambda item: (item[1], item[0], item[2]))
        controller_entries[class_name] = {
            'file': controller_file,
            'base_paths': base_paths,
            'entries': deduped,
        }

    lines_out: list[str] = []
    lines_out.append('# Backend API Catalog')
    lines_out.append('')
    for controller_name in sorted(controller_entries):
        controller = controller_entries[controller_name]
        lines_out.append(f'## {controller_name}')
        for http_method, path, method_name in controller['entries']:
            if method_name:
                lines_out.append(f'- {http_method} `{path}` - {method_name}')
            else:
                lines_out.append(f'- {http_method} `{path}`')
        lines_out.append('')

    OUTPUT.write_text('\n'.join(lines_out), encoding='ascii')


if __name__ == '__main__':
    main()
