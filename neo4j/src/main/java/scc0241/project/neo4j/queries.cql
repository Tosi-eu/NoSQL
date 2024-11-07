# select de tudo na tabela
MATCH(N) RETURN N

# select de tudo de um usuário
match (n:User{name: 'Isabela'}) return n

# select de todos amigos de usuário
MATCH(N:User{name: 'Eva'})-[:FRIEND]->(friends) RETURN friends as amigos

# select de todos os posts de um usuário
MATCH(N:User{name: 'Eva'})-[:POSTED]->(posts)  RETURN posts.content as conteúdo

# select de comentários de um usuário feitos em post
MATCH(N:User{name: 'Eva'})-[:COMMENTED]->(comments)-[:ON_POST]->(post) RETURN comments.content as conteudo, post.content as post

# select de todos comentários, posts e amigos de um dado usuário
MATCH (u:User {name: 'Eva'})
OPTIONAL MATCH (u)-[:POSTED]->(post:Post)
OPTIONAL MATCH (u)-[:COMMENTED]->(comentario:Comment)
OPTIONAL MATCH (u)-[:FRIEND]->(amigo:User)
RETURN u.name AS Usuario, 
       COLLECT(DISTINCT post) AS Posts, 
       COLLECT(DISTINCT comentario) AS Comentarios,
       COLLECT(DISTINCT amigo) AS Amigos;

# select de todos amigos de um usuário (lista)
MATCH (u:User {name: 'Eva'})-[:FRIEND]->(friend)
RETURN u.name AS Usuario, COLLECT(friend.name) AS Amigos;

# select de todos amigos de um usuário
MATCH (u:User {name: 'Eva'})-[:FRIEND]->(friend)
RETURN u.name AS Usuario, COLLECT(friend) AS Amigos;

# select de todas relações FRIEND do usuário
MATCH(N:User{name:'Eva'})-[:FRIEND]->(friends) RETURN N, friends

# select da relação POST de um usuário
MATCH(N:User{name:'Eva'})-[:POSTED]->(posts) RETURN N, posts

# select da relçaõ COMMENT de usuário
MATCH(N:User{name:'Eva'})-[:COMMENTED]->(comments) RETURN N, comments

# select de pessoas que seguem mas que o usuário não tem amizade
MATCH (follower:User)-[:FRIEND]->(p:User {name: "Eva"})
RETURN follower AS Seguidor;


