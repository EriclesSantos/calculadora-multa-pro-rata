# Capturas da demonstração

Todas as imagens usam dados sintéticos. As três primeiras são capturas do aplicativo em execução. As imagens 4 a 7 são capturas de uma visualização local dos dados extraídos do arquivo `exemplos/resultado-demonstracao.xlsx`, efetivamente gerado pelo aplicativo. Esse visualizador não é o Microsoft Excel e mostra um exemplo fixo. As imagens 8 a 10 são capturas do código da edição demonstrativa aberto no VS Code.

## 1. Formulário e parâmetros

Aviso de demonstração, datas preenchidas e escolha do tipo de cálculo.

![Formulário do aplicativo](screenshots/01-formulario.png)

## 2. Operadoras e arquivo de entrada

Operadoras ALFA, BETA e GAMA, tarifas fictícias e TXT sintético selecionado.

![Operadoras e arquivo sintético](screenshots/02-operadoras.png)

## 3. Processamento concluído

Confirmação exibida pelo aplicativo após o download do relatório.

![Confirmação de processamento](screenshots/03-processamento-concluido.png)

## 4. Resumo da multa

Quantidades e valores por operadora, com aviso de resultado parcial.

![Resumo de multa](screenshots/04-resumo-multa.png)

## 5. Pró-rata e total

Multa de R$ 586,60, pró-rata de R$ 275,81 e total simulado de R$ 862,41.

![Pró-rata e total simulado](screenshots/05-resumo-total.png)

## 6. Resultados por linha

Identificadores fictícios, datas, dias restantes, valores e uma linha com situação pendente.

![Resultado por linha](screenshots/06-resultado-por-linha.png)

## 7. Inconsistências

Exemplos propositais de operadora desconhecida e data inválida para demonstrar a conferência.

![Inconsistências do exemplo](screenshots/07-inconsistencias.png)

## 8. Upload no backend

Controller que recebe o arquivo e os parâmetros do formulário.

![UploadController no VS Code](screenshots/08-codigo-upload.png)

## 9. Cálculo proporcional

Dias restantes, valor mensal, divisão por 30 e aplicação do percentual didático.

![MultaService no VS Code](screenshots/09-codigo-calculo.png)

## 10. Leitura dos formatos

FileService identificando os formatos de entrada e iniciando a leitura.

![FileService no VS Code](screenshots/10-codigo-leitura.png)
