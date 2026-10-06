INT 09H     
MOV AX, DX  
MOV BX, 5
CMP AX, BX  
JE 3        
MOV DX, 0   
INT 10H     
INT 20H     
MOV DX, 1   
INT 10H     
INT 20H     