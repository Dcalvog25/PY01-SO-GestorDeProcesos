MOV DX, "datos.txt" 
MOV AH, 3ch         
INT 21H             
MOV AH, 3dh        
INT 21H             
MOV AL, 99          
MOV AH, 40h         
INT 21H             
MOV AH, 4dh        
INT 21H             
MOV DX, AL          
INT 10H            
INT 20H