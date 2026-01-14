extends Control

@onready var input_text: TextEdit = $VBoxContainer/InputText
@onready var load_file_button: Button = $VBoxContainer/HBoxContainer/LoadFileButton
@onready var translate_button: Button = $VBoxContainer/HBoxContainer/TranslateButton
@onready var output_text: TextEdit = $VBoxContainer/OutputText
@onready var error_label: Label = $VBoxContainer/ErrorLabel
@onready var file_dialog: FileDialog = $FileDialog

func _ready():
	# Подключаем сигналы
	load_file_button.connect("pressed", Callable(self, "_on_load_file_pressed"))
	translate_button.connect("pressed", Callable(self, "_on_translate_pressed"))
	file_dialog.connect("file_selected", Callable(self, "_on_file_selected"))

func _on_load_file_pressed():
	# Открываем диалог выбора файла
	file_dialog.popup_centered()

func _on_file_selected(path: String):
	# Загружаем содержимое файла в TextEdit
	var file = FileAccess.open(path, FileAccess.READ)
	if file:
		input_text.text = file.get_as_text()
		file.close()
	else:
		error_label.text = "Ошибка: Не удалось открыть файл."

func _on_translate_pressed():
	# Получаем код из ввода
	var code = input_text.text
	if code.strip_edges() == "":
		error_label.text = "Ошибка: Введите код или загрузите файл."
		return
	
	# Очищаем предыдущие сообщения
	error_label.text = ""
	output_text.text = ""
	
	# Вызываем трансляцию (интеграция с Java ниже)
	var result = translate_code(code)
	if result.has("error"):
		error_label.text = "Ошибка трансляции: " + result["error"]
	else:
		output_text.text = result["output"]

func translate_code(code: String) -> Dictionary:
	# Сохраняем код во временный файл
	var input_file = "temp_input.txt"
	var output_file = "temp_output.txt"
	var error_file = "temp_error.txt"
	
	var file = FileAccess.open(input_file, FileAccess.WRITE)
	file.store_string(code)
	file.close()
	
	# Запускаем Java-приложение (предполагаем, что translator.jar принимает --input и --output)
	var exit_code = OS.execute("java", ["-jar", "out/artifacts/GLSL_to_HLSL_translator_jar/GLSL-to-HLSL-translator.jar", "--input", input_file, "--output", output_file, "--error", error_file], [], true)
	
	# Читаем результат
	var output = ""
	var error = ""
	if exit_code == 0:
		file = FileAccess.open(output_file, FileAccess.READ)
		if file:
			output = file.get_as_text()
			file.close()
	else:
		file = FileAccess.open(error_file, FileAccess.READ)
		if file:
			error = file.get_as_text()
			file.close()
	
	# Удаляем временные файлы
	#DirAccess.remove_absolute(input_file)
	#DirAccess.remove_absolute(output_file)
	#DirAccess.remove_absolute(error_file)
	
	return {"output": output, "error": error}
