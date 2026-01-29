package main;


import lexer.GLSLLexer;
import lexer.Token;
import parser.*;
import semantics.SemanticAnalyzer;
import generator.HLSLGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;

public class GLSLtoHLSLTranslator {
    
    private static final Map<String, String> TYPE_MAPPING = new HashMap<>();
    private static final Map<String, String> FUNCTION_MAPPING = new HashMap<>();
    private static final Map<String, String> KEYWORD_MAPPING = new HashMap<>();
    
    static {
        // Типы данных
        TYPE_MAPPING.put("vec2", "float2");
        TYPE_MAPPING.put("vec3", "float3");
        TYPE_MAPPING.put("vec4", "float4");
        TYPE_MAPPING.put("mat2", "float2x2");
        TYPE_MAPPING.put("mat3", "float3x3");
        TYPE_MAPPING.put("mat4", "float4x4");
        TYPE_MAPPING.put("sampler2D", "Texture2D");
        TYPE_MAPPING.put("samplerCube", "TextureCube");
        
        // Встроенные функции
        FUNCTION_MAPPING.put("texture\\s*\\(", "tex2D(");
        FUNCTION_MAPPING.put("texture2D\\s*\\(", "tex2D(");
        FUNCTION_MAPPING.put("textureCube\\s*\\(", "texCUBE(");
        FUNCTION_MAPPING.put("mix\\s*\\(", "lerp(");
        
        // Ключевые слова и модификаторы
        KEYWORD_MAPPING.put("varying", "static");
        KEYWORD_MAPPING.put("attribute", "");
        KEYWORD_MAPPING.put("uniform", "");
        KEYWORD_MAPPING.put("in ", "");
        KEYWORD_MAPPING.put("out ", "");
    }
    
    public static String translate(String glslCode) {
        String hlslCode = glslCode;
        
        // Замена типов
        for (Map.Entry<String, String> entry : TYPE_MAPPING.entrySet()) {
            hlslCode = hlslCode.replaceAll(entry.getKey(), entry.getValue());
        }
        
        // Замена функций
        for (Map.Entry<String, String> entry : FUNCTION_MAPPING.entrySet()) {
            hlslCode = hlslCode.replaceAll(entry.getKey(), entry.getValue());
        }
        
        // Замена ключевых слов
        for (Map.Entry<String, String> entry : KEYWORD_MAPPING.entrySet()) {
            hlslCode = hlslCode.replaceAll(entry.getKey(), entry.getValue());
        }
        
        // Семантика для входов/выходов (упрощённо)
        hlslCode = addSemantics(hlslCode);
        
        // Удаление специфичных GLSL конструкций
        hlslCode = hlslCode.replace("gl_Position", "output.position");
        hlslCode = hlslCode.replace("gl_FragColor", "output.color");
        
        return hlslCode;
    }
    
    private static String addSemantics(String code) {
        // Пример: преобразование 'in vec3 position;' -> 'float3 position : POSITION;'
        Pattern inPattern = Pattern.compile("\\s*(in\\s+)(\\w+)\\s+(\\w+)\\s*;");
        Matcher m = inPattern.matcher(code);
        StringBuffer sb = new StringBuffer();
        
        while (m.find()) {
            String type = TYPE_MAPPING.getOrDefault(m.group(2), m.group(2));
            String varName = m.group(3);
            String semantic = inferSemantic(varName);
            m.appendReplacement(sb, type + " " + varName + " : " + semantic + ";");
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    private static String inferSemantic(String varName) {
        if (varName.contains("position")) return "POSITION";
        if (varName.contains("normal")) return "NORMAL";
        if (varName.contains("uv") || varName.contains("texcoord")) return "TEXCOORD";
        if (varName.contains("color")) return "COLOR";
        return "TEXCOORD0"; // По умолчанию
    }

        public static void main(String[] args) {

            System.out.println("=== GLSL → HLSL Translator (Test Mode) ===\n");

            // ---------------------------------------------------------
            // 1. ВСТРОЕННЫЙ GLSL-КОД ДЛЯ ТЕСТИРОВАНИЯ
            // ---------------------------------------------------------
            String glslCode = """
            #version 330

            uniform mat4 MVP;

            in vec3 position;
            in vec2 texCoord;
            out vec2 uv;

            void main() {
                gl_Position2 = MVP * vec4(position, 1.0);
                uv = texCoord;
            }
            """;

            System.out.println("=== GLSL INPUT ===");
            System.out.println(glslCode);
            System.out.println("==================\n");

            // ---------------------------------------------------------
            // 2. ЛЕКСЕР
            // ---------------------------------------------------------
            GLSLLexer lexer = new GLSLLexer(glslCode);
            List<Token> tokens;

            try {
                tokens = lexer.tokenize();
            } catch (Exception e) {
                System.err.println("Lexer error:");
                e.printStackTrace();
                return;
            }

            System.out.println("Lexical analysis OK. Tokens: " + tokens.size());

            // ---------------------------------------------------------
            // 3. ПАРСЕР
            // ---------------------------------------------------------
            GLSLParser parser = new GLSLParser(tokens);
            GLSLParser.Program ast;

            try {
                ast = parser.parseProgram();
            } catch (Exception e) {
                System.err.println("Parser crashed:");
                e.printStackTrace();
                return;
            }

            if (!parser.getErrors().isEmpty()) {
                System.out.println("\n=== PARSER ERRORS ===");
                parser.getErrors().forEach(System.out::println);
                System.out.println("Translation aborted.");
                return;
            }

            System.out.println("Parsing completed successfully.");

            // ---------------------------------------------------------
            // 4. ГЕНЕРАЦИЯ HLSL (пока заглушка)
            // ---------------------------------------------------------
            HLSLGenerator generator = new HLSLGenerator();
            String hlsl = generator.generate(ast);


            /*try {
                hlsl = generator.generate(ast);
            } catch (Exception e) {
                System.err.println("HLSL generation error:");
                e.printStackTrace();
                return;
            }*/

            // ---------------------------------------------------------
            // 5. ВЫВОД РЕЗУЛЬТАТА
            // ---------------------------------------------------------
            System.out.println("\n=== HLSL OUTPUT ===");
            System.out.println(hlsl);
            System.out.println("====================");

            System.out.println("\n=== Translation completed ===");
        }
    }


